package com.example.expensesplitter;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test: starts the whole application with the in-memory H2 database and calls the
 * real endpoints through MockMvc (no network port needed).
 * Every test uses its own users, so tests don't interfere with each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseSplitterApiTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------- small helpers to keep the tests readable ----------

    /** POSTs JSON and returns the "id" of the created resource. */
    private long postAndGetId(String url, String json) throws Exception {
        String body = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createUser(String name, String email) throws Exception {
        return postAndGetId("/users", "{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}");
    }

    private long createGroup(String name, long... memberIds) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < memberIds.length; i++) {
            ids.append(i == 0 ? "" : ",").append(memberIds[i]);
        }
        return postAndGetId("/groups", "{\"name\":\"" + name + "\",\"memberIds\":[" + ids + "]}");
    }

    private String expenseJson(String description, String amount, long payerId, long... participantIds) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < participantIds.length; i++) {
            ids.append(i == 0 ? "" : ",").append(participantIds[i]);
        }
        return "{\"description\":\"" + description + "\",\"amount\":" + amount
                + ",\"payerId\":" + payerId + ",\"participantIds\":[" + ids + "]}";
    }

    // ---------- tests ----------

    @Test
    @DisplayName("Full flow: users -> group -> expenses -> balances -> settlements")
    void fullFlow() throws Exception {
        long alice = createUser("Alice", "alice.flow@example.com");
        long bob = createUser("Bob", "bob.flow@example.com");
        long charlie = createUser("Charlie", "charlie.flow@example.com");
        long group = createGroup("Goa Trip", alice, bob, charlie);

        // Alice pays 3000 for dinner (shared by all three) -> 1000 each
        mockMvc.perform(post("/groups/" + group + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Dinner", "3000.00", alice, alice, bob, charlie)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(3000.0))
                .andExpect(jsonPath("$.payer.name").value("Alice"))
                .andExpect(jsonPath("$.participants.length()").value(3));

        // Bob pays 900 for a cab (shared by all three) -> 300 each
        mockMvc.perform(post("/groups/" + group + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Cab", "900", bob, alice, bob, charlie)))
                .andExpect(status().isCreated());

        // Alice: +3000 -1000 -300 = +1700 | Bob: +900 -1000 -300 = -400 | Charlie: -1000 -300 = -1300
        mockMvc.perform(get("/groups/" + group + "/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].userName").value("Alice"))
                .andExpect(jsonPath("$[0].balance").value(1700.0))
                .andExpect(jsonPath("$[1].userName").value("Bob"))
                .andExpect(jsonPath("$[1].balance").value(-400.0))
                .andExpect(jsonPath("$[2].userName").value("Charlie"))
                .andExpect(jsonPath("$[2].balance").value(-1300.0));

        // Charlie owes the most, so he pays first. Two payments settle everything.
        mockMvc.perform(get("/groups/" + group + "/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fromUserName").value("Charlie"))
                .andExpect(jsonPath("$[0].toUserName").value("Alice"))
                .andExpect(jsonPath("$[0].amount").value(1300.0))
                .andExpect(jsonPath("$[1].fromUserName").value("Bob"))
                .andExpect(jsonPath("$[1].toUserName").value("Alice"))
                .andExpect(jsonPath("$[1].amount").value(400.0));
    }

    @Test
    @DisplayName("An expense that does not divide evenly still balances to zero")
    void unevenSplitBalancesToZero() throws Exception {
        long a = createUser("Ann", "ann.uneven@example.com");
        long b = createUser("Ben", "ben.uneven@example.com");
        long c = createUser("Cat", "cat.uneven@example.com");
        long group = createGroup("Uneven", a, b, c);

        mockMvc.perform(post("/groups/" + group + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Snacks", "100.00", a, a, b, c)))
                .andExpect(status().isCreated());

        // Shares are 33.34 / 33.33 / 33.33 -> Ann +66.66, Ben -33.33, Cat -33.33
        mockMvc.perform(get("/groups/" + group + "/balances"))
                .andExpect(jsonPath("$[0].balance").value(66.66))
                .andExpect(jsonPath("$[1].balance").value(-33.33))
                .andExpect(jsonPath("$[2].balance").value(-33.33));
    }

    @Test
    @DisplayName("Invalid user -> 400 with field errors")
    void validationError() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("Duplicate email -> 409")
    void duplicateEmail() throws Exception {
        createUser("Dup", "dup@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup Two\",\"email\":\"DUP@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Unknown group -> 404")
    void unknownGroup() throws Exception {
        mockMvc.perform(get("/groups/999999/balances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Malformed JSON -> 400")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Payer who is not in the group -> 400")
    void payerNotInGroup() throws Exception {
        long member = createUser("Member", "member.payer@example.com");
        long outsider = createUser("Outsider", "outsider.payer@example.com");
        long group = createGroup("Closed group", member);

        mockMvc.perform(post("/groups/" + group + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Taxi", "20.00", outsider, member)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Add and remove members; a member with expenses cannot be removed")
    void membersLifecycle() throws Exception {
        long u1 = createUser("One", "one.members@example.com");
        long u2 = createUser("Two", "two.members@example.com");
        long group = createGroup("Members", u1);

        // add u2 -> 200, adding again -> 409
        mockMvc.perform(post("/groups/" + group + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + u2 + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
        mockMvc.perform(post("/groups/" + group + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + u2 + "}"))
                .andExpect(status().isConflict());

        // remove u2 (no expenses yet) -> 200
        mockMvc.perform(delete("/groups/" + group + "/members/" + u2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));

        // u1 pays an expense, so u1 can no longer be removed -> 409
        mockMvc.perform(post("/groups/" + group + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Coffee", "5.00", u1, u1)))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/groups/" + group + "/members/" + u1))
                .andExpect(status().isConflict());
    }
}

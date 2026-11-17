const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profiles, reports, or chat messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reports").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chat_messages").get());

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Hendra Wijaya",
      nrp: "SEC-BPS-042",
      role: "Danru Plot BPS",
      activeShift: "Shift 1",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Authenticated owner: valid OfficerProfile create and partial update succeed; ghost/tampered fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Hendra Wijaya",
      nrp: "SEC-BPS-042",
      role: "Danru Plot BPS",
      activeShift: "Shift 1",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeShift: "Shift 2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid role enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "Superuser",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: valid SecurityReport create, list, update, delete succeed; invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty list query succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").orderBy("createdAt", "desc").get()
  );

  // Valid create (no 'id' field because @DocumentId is used in Kotlin)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").set({
      userId: ALICE_UID,
      category: 1,
      time: "07:05",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Apel serah terima jaga pagi di Pos Utama Plot BPS.",
      photos: ["base64_photo_sample"],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject unexpected @DocumentId 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_2").set({
      id: "rep_2",
      userId: ALICE_UID,
      category: 2,
      time: "08:00",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Patroli gedung.",
      photos: [],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject more than 4 photos
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_3").set({
      userId: ALICE_UID,
      category: 2,
      time: "08:30",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Patroli dengan 5 foto.",
      photos: ["p1", "p2", "p3", "p4", "p5"],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_4").set({
      userId: ALICE_UID,
      category: 1,
      time: "09:00",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Future timestamp.",
      photos: [],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Valid partial update & delete
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").update({
      status: "Dalam Pemantauan",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").delete()
  );
});

test("Authenticated owner: valid ChatMessage create and list succeed; empty payload and updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").orderBy("createdAt", "asc").get()
  );

  // Text message (optional fields omitted just like Kotlin .filterValues { it != null })
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_1").set({
      userId: ALICE_UID,
      senderName: "Hendra Wijaya",
      senderRole: "Danru Plot BPS",
      fromOfficer: true,
      time: "07:15",
      text: "Lapor komandan, pos depan aman terkendali.",
      audioType: "none",
      createdAt: created,
    })
  );

  // Voice note message
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_2").set({
      userId: ALICE_UID,
      senderName: "Hendra Wijaya",
      senderRole: "Danru Plot BPS",
      fromOfficer: true,
      time: "07:20",
      text: "",
      audioType: "recorded",
      audioData: "base64_m4a_payload",
      audioDuration: "00:08",
      createdAt: created,
    })
  );

  // Empty message without text, image, or audio must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_3").set({
      userId: ALICE_UID,
      senderName: "Hendra Wijaya",
      senderRole: "Danru Plot BPS",
      fromOfficer: true,
      time: "07:21",
      text: "",
      audioType: "none",
      createdAt: created,
    })
  );

  // Chat messages are immutable (update must fail)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg_1").update({
      text: "Edited",
    })
  );
});

test("Cross-user isolation: Alice cannot read, list, or write Bob's profile, reports, or chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const past = new Date(Date.now() - 10000);
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bapak Suroto",
      nrp: "SEC-BPS-001",
      role: "Korlap Security BPS",
      activeShift: "Shift 1",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("reports").doc("bob_rep").set({
      userId: BOB_UID,
      category: 1,
      time: "07:00",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Bob report",
      photos: [],
      officerName: "Bapak Suroto",
      officerNrp: "SEC-BPS-001",
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("reports").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("reports").doc("bob_rep").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("chat_messages").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("reports").doc("bob_rep").delete()
  );
});

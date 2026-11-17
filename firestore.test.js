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

test("Unauthenticated user: cannot read or write profiles, reports, chat rooms, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reports").get());
  await assertFails(unauthDb.collection("chat_rooms").doc("PLOT-BPS").get());
  await assertFails(
    unauthDb.collection("chat_rooms").doc("PLOT-BPS").collection("messages").where("roomCode", "==", "PLOT-BPS").get()
  );

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

test("Authenticated owner: OfficerProfile create, presence/roomCode update succeed; ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Hendra Wijaya",
      nrp: "SEC-BPS-042",
      role: "Danru Plot BPS",
      activeShift: "Shift 1",
      activeRoomCode: "PLOT-BPS",
      onlineStatus: "online",
      lastSeenAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeShift: "Shift 2",
      activeRoomCode: "PATROLI-01",
      onlineStatus: "offline",
      lastSeenAt: new Date(Date.now() - 1000),
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

  // Reject invalid roomCode
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeRoomCode: "a",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: SecurityReport with photoCaptions and reportDate succeed; invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").orderBy("createdAt", "desc").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").set({
      userId: ALICE_UID,
      category: 1,
      time: "07:05",
      reportDate: "2026-11-17",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Apel serah terima jaga pagi di Pos Utama Plot BPS.",
      photos: ["base64_photo_sample"],
      photoCaptions: ["Foto apel pagi bersama Danru"],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid reportDate format
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_bad_date").set({
      userId: ALICE_UID,
      category: 1,
      time: "07:05",
      reportDate: "17-11-2026",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Bad date.",
      photos: [],
      photoCaptions: [],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject >4 captions
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_bad_cap").set({
      userId: ALICE_UID,
      category: 2,
      time: "08:30",
      reportDate: "2026-11-17",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Patroli.",
      photos: ["p1"],
      photoCaptions: ["c1", "c2", "c3", "c4", "c5"],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").delete()
  );
});

test("Shared ChatRoom: create/get by code, join RoomMember with online presence, send/list messages with imageCaption", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 10000);
  const roomCode = "PATROLI-01";

  // Room list enumeration must fail
  await assertFails(aliceDb.collection("chat_rooms").get());

  // Creating a member/message for nonexistent room must fail
  await assertFails(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      roomCode,
      name: "Hendra Wijaya",
      nrp: "SEC-BPS-042",
      role: "Danru Plot BPS",
      activeShift: "Shift 1",
      onlineStatus: "online",
      lastSeenAt: past,
      joinedAt: past,
    })
  );

  // Alice creates the chat room
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).set({
      roomCode,
      title: "Regu Patroli Malam",
      createdBy: ALICE_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob can get the room if Bob knows the exact code
  await assertSucceeds(bobDb.collection("chat_rooms").doc(roomCode).get());

  // Alice and Bob each write ONLY their own member presence document
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      roomCode,
      name: "Hendra Wijaya",
      nrp: "SEC-BPS-042",
      role: "Danru Plot BPS",
      activeShift: "Shift 1",
      onlineStatus: "online",
      lastSeenAt: past,
      joinedAt: past,
    })
  );
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("members").doc(BOB_UID).set({
      userId: BOB_UID,
      roomCode,
      name: "Bapak Suroto",
      nrp: "SEC-BPS-001",
      role: "Korlap Security BPS",
      activeShift: "Shift 1",
      onlineStatus: "online",
      lastSeenAt: past,
      joinedAt: past,
    })
  );

  // Alice CANNOT overwrite Bob's online status
  await assertFails(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("members").doc(BOB_UID).update({
      onlineStatus: "offline",
      lastSeenAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered member list succeeds; unfiltered list fails
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("members").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("members").get()
  );

  // Alice sends photo message with imageCaption
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      roomCode,
      senderName: "Hendra Wijaya",
      senderRole: "Danru Plot BPS",
      fromOfficer: true,
      time: "08:15",
      text: "Cek gerbang utama",
      imageData: "base64_img",
      imageCaption: "Pintu gerbang utama terkunci aman",
      audioType: "none",
      createdAt: past,
    })
  );

  // Bob can list room messages with roomCode filter, cannot delete Alice's message
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").get()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_1").delete()
  );

  // Bob cannot spoof Alice's UID when sending message
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_spoof").set({
      userId: ALICE_UID,
      roomCode,
      senderName: "Hendra Wijaya",
      senderRole: "Danru Plot BPS",
      fromOfficer: true,
      time: "08:20",
      text: "Spoof",
      audioType: "none",
      createdAt: past,
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's private profile or reports", async () => {
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
});

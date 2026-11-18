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
  // Alice edits her own message with emoji
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_1").update({
      text: "Cek gerbang utama aman terkendali 👍🛡️",
      edited: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot edit Alice's message
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_1").update({
      text: "Edited by Bob",
      edited: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Bob sends a message then Alice (room creator) can clear room chat; Bob cannot delete Alice's message
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_bob").set({
      userId: BOB_UID,
      roomCode,
      senderName: "Bapak Suroto",
      senderRole: "Korlap Security BPS",
      fromOfficer: true,
      time: "08:18",
      text: "Siap lanjut patroli 🚓",
      audioType: "none",
      createdAt: past,
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_1").delete()
  );
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("messages").doc("msg_bob").delete()
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

test("Cross-user isolation: Alice cannot read or write Bob's private profile, reports, or attendance sheets", async () => {
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
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("activity_sheets").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("attendance_sheets").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("pju_sheets").get());
});

test("Authenticated owner: DailyActivitySheet and 15-member AttendanceSheet CRUD succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity_sheets").doc("sheet_1").set({
      userId: ALICE_UID,
      docNumber: "001/BPS",
      sheetDate: "2026-11-18",
      shift: "Shift 2",
      personnelRows: [
        { nama: "Bapak Siswandi", jabatan: "DANRU", plot: "B", kehadiran: "Hadir", ket: "" }
      ],
      inventoryRows: [
        { inventaris: "B. Jurnal * B. Tamu * B. Inventaris", jumlah: "1 * 1 * 1", kondisi: "Baik", keterangan: "Lengkap" }
      ],
      activityRows: [
        { jam: "15.00", uraian: "Menerima tugas dan tanggung jawab kepada shif II dalam keadaan aman." }
      ],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity_sheets").doc("sheet_1").update({
      docNumber: "002/BPS",
      activityRows: [
        { jam: "15.00", uraian: "Serah terima aman" },
        { jam: "15.30", uraian: "Standby di Portal utama BPS" }
      ],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // AttendanceSheet with 15 members
  const fifteenRows = Array.from({ length: 15 }, (_, i) => ({
    nama: `Anggota ${i + 1}`,
    nrp: `SEC-BPS-${String(i + 1).padStart(3, "0")}`,
    jabatan: i === 0 ? "DANRU" : "ANGGOTA",
    plot: "BPS",
    status: "H",
    jamMasuk: "07.00",
    jamPulang: "15.00",
    recordedAt: "07:01",
    keterangan: "",
  }));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance_sheets").doc("att_1").set({
      userId: ALICE_UID,
      sheetDate: "2026-11-18",
      shift: "Shift 1",
      periodLabel: "Regu Plot BPS",
      commanderName: "Bapak Siswandi",
      rows: fifteenRows,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance_sheets").doc("att_1").update({
      rows: fifteenRows,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance_sheets").doc("att_bad").set({
      userId: ALICE_UID,
      sheetDate: "18-11-2026",
      shift: "Shift 1",
      periodLabel: "Regu",
      commanderName: "Danru",
      rows: [],
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner: PjuChecklistSheet CRUD and validation succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const defaultClusters = [
    { clusterName: "PJU KRISAN", checks: { "16": "✓|✓|✓|✓", "18": "✓|!|✓|✓" } },
    { clusterName: "PJU CEMPAKA", checks: { "16": "✓|✓|✓|✓" } },
    { clusterName: "PJU CENDANA", checks: {} },
    { clusterName: "PJU CEMARA", checks: {} },
    { clusterName: "PJU FLAMBOYAN 1", checks: {} },
    { clusterName: "PJU FLAMBOYAN 2", checks: {} },
    { clusterName: "PJU FLAMBOYAN FASUM", checks: {} },
    { clusterName: "PJU AKASIA", checks: {} },
    { clusterName: "RUKO", checks: {} },
  ];

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pju_sheets").doc("pju_1").set({
      userId: ALICE_UID,
      monthYear: "November 2026",
      sheetMonth: 11,
      sheetYear: 2026,
      periodStartDay: 16,
      periodEndDay: 30,
      clusters: defaultClusters,
      issues: [
        {
          tanggal: "2026-11-18",
          cluster: "PJU KRISAN",
          titikLampu: "Lampu 2",
          keluhan: "Lampu berkedip/redup sejak pukul 19.00 WIB",
          tindakLanjut: "Dilaporkan ke teknisi BPS",
          status: "Belum Ditangani",
        },
      ],
      notes: "Pemeriksaan rutin dilakukan maksimal 2 hari sekali.",
      responsibleNames: "R. MOH. GANDI / NERI WELAS ANTORO",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pju_sheets").doc("pju_1").update({
      notes: "Lampu Krisan 2 sudah dicek ulang.",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid periodEndDay < periodStartDay
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pju_sheets").doc("pju_bad").set({
      userId: ALICE_UID,
      monthYear: "November 2026",
      sheetMonth: 11,
      sheetYear: 2026,
      periodStartDay: 20,
      periodEndDay: 15,
      clusters: [],
      issues: [],
      notes: "",
      responsibleNames: "R. MOH. GANDI / NERI WELAS ANTORO",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pju_sheets").doc("pju_1").delete()
  );
});

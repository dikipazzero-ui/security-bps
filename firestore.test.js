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
      shift: "Shift 1",
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
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("quick_notes").get());
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
      lastShift: "Shift 2",
      lastUpdatedTime: "19:30",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pju_sheets").doc("pju_1").update({
      notes: "Lampu Krisan 2 sudah dicek ulang.",
      lastShift: "Shift 3",
      lastUpdatedTime: "23:15",
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

test("Authenticated owner: QuickNote CRUD with shift and timestamp succeed; invalid shift fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quick_notes").doc("qn_1").set({
      userId: ALICE_UID,
      sourceTab: "Chat",
      title: "Catatan Patroli Malam",
      content: "Pintu gerbang belakang terkunci ganda dan lampu PJU Cendana normal.",
      noteDate: "2026-11-18",
      noteTime: "23:40",
      shift: "Shift 3",
      officerName: "Hendra Wijaya",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("quick_notes").doc("qn_bad").set({
      userId: ALICE_UID,
      sourceTab: "Chat",
      title: "Invalid Shift",
      content: "Test",
      noteDate: "2026-11-18",
      noteTime: "23:40",
      shift: "Shift 9",
      officerName: "Hendra Wijaya",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quick_notes").doc("qn_1").delete()
  );
});

test("Shared Room Workspace (1 database, 1 room): all logged-in officers can monitor reports, sheets, attendance, PJU, and notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const roomCode = "PLOT-BPS";
  const created = new Date(Date.now() - 10000);
  const future = new Date(Date.now() + 3600 * 1000);

  // Unauthenticated access to shared workspace subcollections must fail
  await assertFails(
    unauthDb.collection("chat_rooms").doc(roomCode).collection("reports").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    unauthDb.collection("chat_rooms").doc(roomCode).collection("activity_sheets").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    unauthDb.collection("chat_rooms").doc(roomCode).collection("attendance_sheets").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    unauthDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").where("roomCode", "==", roomCode).get()
  );
  await assertFails(
    unauthDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").where("roomCode", "==", roomCode).get()
  );

  // Writing to a nonexistent room must fail
  await assertFails(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_no_room").set({
      userId: ALICE_UID,
      roomCode,
      category: 1,
      time: "07:05",
      reportDate: "2026-11-18",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Test tanpa ruang",
      photos: [],
      photoCaptions: [],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice creates shared room PLOT-BPS
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).set({
      roomCode,
      title: "Channel Koordinasi Plot BPS",
      createdBy: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice creates shared report, activity sheet, attendance sheet, PJU sheet, and quick note
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_shared_1").set({
      userId: ALICE_UID,
      roomCode,
      category: 2,
      time: "09:15",
      reportDate: "2026-11-18",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Patroli keliling gedung BPS terpantau aman terkendali.",
      photos: [],
      photoCaptions: [],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("activity_sheets").doc("sheet_shared_1").set({
      userId: ALICE_UID,
      roomCode,
      docNumber: "001",
      sheetDate: "2026-11-18",
      shift: "Shift 1",
      personnelRows: [
        { nama: "Hendra Wijaya", jabatan: "DANRU", plot: "BPS", kehadiran: "Hadir", ket: "" }
      ],
      inventoryRows: [
        { inventaris: "HT Motorolla", jumlah: "3", kondisi: "Baik", keterangan: "Lengkap" }
      ],
      activityRows: [
        { jam: "07.00", uraian: "Apel serah terima regu Shift 1" }
      ],
      officerName: "Hendra Wijaya",
      officerNrp: "SEC-BPS-042",
      createdAt: created,
      updatedAt: created,
    })
  );

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
    aliceDb.collection("chat_rooms").doc(roomCode).collection("attendance_sheets").doc("att_shared_1").set({
      userId: ALICE_UID,
      roomCode,
      sheetDate: "2026-11-18",
      shift: "Shift 1",
      periodLabel: "Regu Plot BPS",
      commanderName: "Hendra Wijaya",
      rows: fifteenRows,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").doc("pju_shared_1").set({
      userId: ALICE_UID,
      roomCode,
      monthYear: "November 2026",
      sheetMonth: 11,
      sheetYear: 2026,
      periodStartDay: 16,
      periodEndDay: 30,
      clusters: [
        { clusterName: "PJU KRISAN", checks: { "18": "✓|✓|✓|✓" } }
      ],
      issues: [],
      notes: "Cek rutin PJU.",
      responsibleNames: "R. MOH. GANDI / NERI WELAS ANTORO",
      lastShift: "Shift 1",
      lastUpdatedTime: "07:30",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_shared_1").set({
      userId: ALICE_UID,
      roomCode,
      sourceTab: "LAPORAN",
      title: "Catatan Serah Terima",
      content: "Kunci gudang arsip diserahkan lengkap ke Shift 2.",
      noteDate: "2026-11-18",
      noteTime: "14:55",
      shift: "Shift 1",
      officerName: "Hendra Wijaya",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unfiltered list queries must fail; filtered roomCode list queries succeed for Bob
  for (const col of ["reports", "activity_sheets", "attendance_sheets", "pju_sheets", "quick_notes"]) {
    await assertFails(bobDb.collection("chat_rooms").doc(roomCode).collection(col).get());
    await assertSucceeds(
      bobDb.collection("chat_rooms").doc(roomCode).collection(col).where("roomCode", "==", roomCode).get()
    );
  }

  // Bob can get single docs created by Alice
  await assertSucceeds(bobDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_shared_1").get());
  await assertSucceeds(bobDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_shared_1").get());

  // Bob CANNOT spoof Alice's userId or mismatch roomCode on create
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_spoof").set({
      userId: ALICE_UID,
      roomCode,
      category: 1,
      time: "10:00",
      reportDate: "2026-11-18",
      shift: "Shift 1",
      status: "Aman & Terkendali",
      description: "Spoof",
      photos: [],
      photoCaptions: [],
      officerName: "Bapak Suroto",
      officerNrp: "SEC-BPS-001",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_wrong_room").set({
      userId: BOB_UID,
      roomCode: "OTHER-ROOM",
      sourceTab: "LAPORAN",
      title: "Mismatch",
      content: "Mismatch",
      noteDate: "2026-11-18",
      noteTime: "10:00",
      shift: "Shift 1",
      officerName: "Bapak Suroto",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob CAN collaboratively update shared team sheets (activity, attendance, PJU)
  const updateTime = new Date(Date.now() - 1000);
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("activity_sheets").doc("sheet_shared_1").update({
      activityRows: [
        { jam: "07.00", uraian: "Apel serah terima regu Shift 1" },
        { jam: "15.00", uraian: "Dilanjutkan oleh regu Shift 2 (Bapak Suroto)" }
      ],
      officerName: "Bapak Suroto",
      officerNrp: "SEC-BPS-001",
      updatedAt: updateTime,
    })
  );
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("attendance_sheets").doc("att_shared_1").update({
      shift: "Shift 2",
      rows: fifteenRows,
      updatedAt: updateTime,
    })
  );
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").doc("pju_shared_1").update({
      notes: "Dicek ulang Shift 2: semua lampu menyala normal.",
      lastShift: "Shift 2",
      lastUpdatedTime: "19:30",
      updatedAt: updateTime,
    })
  );

  // Collaborative update CANNOT alter userId, roomCode, createdAt, add ghost fields, or use future timestamp
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").doc("pju_shared_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("activity_sheets").doc("sheet_shared_1").update({
      roomCode: "HACK-01",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("attendance_sheets").doc("att_shared_1").update({
      extraField: "ghost",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").doc("pju_shared_1").update({
      notes: "Future timestamp",
      updatedAt: future,
    })
  );

  // Bob CANNOT edit or delete Alice's reports or quick_notes, and CANNOT delete Alice's shared sheets
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_shared_1").update({
      description: "Diubah paksa",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_shared_1").delete()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_shared_1").delete()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("activity_sheets").doc("sheet_shared_1").delete()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("attendance_sheets").doc("att_shared_1").delete()
  );
  await assertFails(
    bobDb.collection("chat_rooms").doc(roomCode).collection("pju_sheets").doc("pju_shared_1").delete()
  );

  // Bob creates his own shared report & quick_note; Bob can delete his own, and Alice (room creator) can also moderate/delete
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_bob_1").set({
      userId: BOB_UID,
      roomCode,
      category: 3,
      time: "16:10",
      reportDate: "2026-11-18",
      shift: "Shift 2",
      status: "Aman & Terkendali",
      description: "Pelayanan tamu sore berjalan tertib.",
      photos: [],
      photoCaptions: [],
      officerName: "Bapak Suroto",
      officerNrp: "SEC-BPS-001",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_bob_1").set({
      userId: BOB_UID,
      roomCode,
      sourceTab: "PJU",
      title: "PJU Sore",
      content: "Semua lampu cluster Akasia menyala normal.",
      noteDate: "2026-11-18",
      noteTime: "18:05",
      shift: "Shift 2",
      officerName: "Bapak Suroto",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Room creator Alice can delete Bob's items in PLOT-BPS
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("reports").doc("rep_bob_1").delete()
  );
  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc(roomCode).collection("quick_notes").doc("qn_bob_1").delete()
  );
});


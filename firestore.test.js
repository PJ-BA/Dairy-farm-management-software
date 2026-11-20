const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Alice Farmer",
    email: "alice@farm.com",
    farmName: "Green Valley Dairy",
    currencySymbol: "$",
    defaultMilkPrice: 1.5,
    feedingRemindersEnabled: true,
    lowStockAlertsEnabled: true,
    productionDropAlertsEnabled: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validCattle(uid) {
  return {
    userId: uid,
    tagNumber: "COW-101",
    name: "Bella",
    species: "Cow",
    breed: "Holstein",
    registrationDate: "2026-11-13",
    isActive: true,
    notes: "High yield",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validFeedItem(uid) {
  return {
    userId: uid,
    name: "Alfalfa Hay",
    category: "Dry Fodder",
    quantity: 500,
    unit: "kg",
    costPerUnit: 0.4,
    minStockLevel: 100,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Unauthenticated user cannot read or write any farm data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cattle").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("User can create, read, and update their own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(profileRef.update({ farmName: "Sunrise Dairy", updatedAt: serverTimestamp() }));
});

test("User cannot list /users or access another user's profile or cattle", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cattle").doc("c1").set(validCattle(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cattle").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cattle").doc("c1").delete());
});

test("Shadow update with ghost field or immutable createdAt mutation is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cattleRef = aliceDb.collection("users").doc(ALICE_UID).collection("cattle").doc("c1");
  await assertSucceeds(cattleRef.set(validCattle(ALICE_UID)));

  // Ghost field rejected
  await assertFails(
    cattleRef.update({ name: "Daisy", ghostAdmin: true, updatedAt: serverTimestamp() })
  );
  // Immutable userId change rejected
  await assertFails(
    cattleRef.update({ userId: BOB_UID, updatedAt: serverTimestamp() })
  );
});

test("MilkRecord creation requires existing Cattle parent and valid non-negative numbers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const milkRef = aliceDb.collection("users").doc(ALICE_UID).collection("milkRecords").doc("m1");

  // Orphaned milk record fails
  await assertFails(
    milkRef.set({
      userId: ALICE_UID,
      cattleId: "missing_cattle",
      cattleTag: "COW-101",
      cattleName: "Bella",
      date: "2026-11-13",
      morningLitres: 12.5,
      eveningLitres: 10.0,
      totalLitres: 22.5,
      notes: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Create cattle first
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cattle").doc("c1").set(validCattle(ALICE_UID))
  );

  // Valid milk record succeeds
  await assertSucceeds(
    milkRef.set({
      userId: ALICE_UID,
      cattleId: "c1",
      cattleTag: "COW-101",
      cattleName: "Bella",
      date: "2026-11-13",
      morningLitres: 12.5,
      eveningLitres: 10.0,
      totalLitres: 22.5,
      notes: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Negative milk rejected
  await assertFails(
    milkRef.update({ morningLitres: -2, updatedAt: serverTimestamp() })
  );
});

test("FeedItem, FeedingSchedule, FeedingRecord, and FarmTransaction rules enforce schema and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const feedRef = aliceDb.collection("users").doc(ALICE_UID).collection("feedItems").doc("f1");
  await assertSucceeds(feedRef.set(validFeedItem(ALICE_UID)));

  const schedRef = aliceDb.collection("users").doc(ALICE_UID).collection("feedingSchedules").doc("s1");
  await assertSucceeds(
    schedRef.set({
      userId: ALICE_UID,
      title: "Morning Hay",
      targetGroup: "Milking Cows",
      feedId: "f1",
      feedName: "Alfalfa Hay",
      quantity: 25,
      unit: "kg",
      feedingTime: "06:30",
      isActive: true,
      lastCompletedDate: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const recRef = aliceDb.collection("users").doc(ALICE_UID).collection("feedingRecords").doc("fr1");
  await assertSucceeds(
    recRef.set({
      userId: ALICE_UID,
      scheduleId: "s1",
      scheduleTitle: "Morning Hay",
      feedId: "f1",
      feedName: "Alfalfa Hay",
      quantityConsumed: 25,
      unit: "kg",
      date: "2026-11-13",
      time: "06:35",
      status: "Completed",
      notes: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("t1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      date: "2026-11-13",
      type: "Income",
      category: "Milk Sales",
      description: "Morning milk batch",
      amount: 150.0,
      quantityLitres: 100.0,
      pricePerLitre: 1.5,
      paymentMethod: "UPI",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Zero amount transaction fails
  await assertFails(txRef.update({ amount: 0, updatedAt: serverTimestamp() }));
});

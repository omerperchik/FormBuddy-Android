import functions from "firebase-functions";

export const processDocument = functions
  .region("us-central1")
  .runWith({ timeoutSeconds: 540, memory: "1GB" })
  .https.onCall(async (data, context) => {
  const {
    base64Document,
    mimeType,
    version: requestedVersionRaw,
    deploymentOwner: deploymentOwnerRaw,
    deploymentName: deploymentNameRaw,
    fast: fastRaw,
  } = data || {};
  if (!base64Document || !mimeType) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required fields: base64Document, mimeType");
  }

  if (typeof base64Document !== "string" || typeof mimeType !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "Invalid types for base64Document or mimeType");
  }

  if (mimeType !== "application/pdf") {
    throw new functions.https.HttpsError("invalid-argument", "Only application/pdf is supported");
  }

  if (base64Document.length > 12_000_000) {
    throw new functions.https.HttpsError("invalid-argument", "Document too large for callable (use upload URL flow)");
  }

  const cfg = functions.config?.() || {};
  const replicateCfg = cfg.replicate || {};
  const token = replicateCfg.token || process.env.REPLICATE_API_TOKEN;
  const requestedVersion = typeof requestedVersionRaw === "string" ? requestedVersionRaw.trim() : "";
  const deploymentOwner = typeof deploymentOwnerRaw === "string" ? deploymentOwnerRaw.trim() : "";
  const deploymentName = typeof deploymentNameRaw === "string" ? deploymentNameRaw.trim() : "";
  const useDeploymentRoute = Boolean(deploymentOwner && deploymentName);
  const version = (requestedVersion && requestedVersion.length <= 200 ? requestedVersion : null)
    || replicateCfg.common_forms_version
    || process.env.REPLICATE_COMMON_FORMS_VERSION
    || "formbuddyai/common_forms:442a7d3e6cdc4ada5038a698143194cbbf0ead2c3ea09d5716acf8423e03fcd7";

  if (!token) {
    throw new functions.https.HttpsError("failed-precondition", "Server is missing Replicate API token");
  }

  const authorizationValue = token.startsWith("r8_") ? `Token ${token}` : `Bearer ${token}`;
  const inputPdfDataUri = `data:${mimeType};base64,${base64Document}`;

  const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  const parseBool = (value) => {
    if (value === true || value === "true" || value === 1 || value === "1") {
      return true;
    }

    if (value === false || value === "false" || value === 0 || value === "0" || value == null) {
      return false;
    }

    return Boolean(value);
  };
  const fast = parseBool(fastRaw);

  const fetchPrediction = async (id) => {
    const res = await fetch(`https://api.replicate.com/v1/predictions/${id}`, {
      method: "GET",
      headers: {
        Authorization: authorizationValue,
      },
    });

    const json = await res.json().catch(() => null);
    if (!res.ok) {
      const detail = json?.detail || json?.error || `Replicate error: ${res.status}`;
      const error = new Error(detail);
      error.status = res.status;
      throw error;
    }
    return json;
  };

  try {
    const input = {
      pdf: inputPdfDataUri,
      return_fillable_pdf: true,
    };

    if (fast) {
      input.fast = true;
    }

    const createPredictionUrl = useDeploymentRoute
      ? `https://api.replicate.com/v1/deployments/${deploymentOwner}/${deploymentName}/predictions`
      : "https://api.replicate.com/v1/predictions";
    const createPredictionBody = useDeploymentRoute ? { input } : { version, input };

    const createRes = await fetch(createPredictionUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: authorizationValue,
      },
      body: JSON.stringify(createPredictionBody),
    });

    const created = await createRes.json().catch(() => null);
    if (!createRes.ok) {
      const detail = created?.detail || created?.error || `Replicate error: ${createRes.status}`;
      const error = new Error(detail);
      error.status = createRes.status;
      throw error;
    }

    let prediction = created;
    const terminalStatuses = new Set(["succeeded", "failed", "canceled"]);

    const startedAt = Date.now();
    const timeoutMs = Number(process.env.REPLICATE_PREDICTION_TIMEOUT_MS || 240000);
    let delayMs = 1000;

    while (prediction?.id && !terminalStatuses.has(prediction.status)) {
      if (Date.now() - startedAt > timeoutMs) {
        throw new Error("Replicate prediction timed out");
      }

      await sleep(delayMs);
      prediction = await fetchPrediction(prediction.id);
      delayMs = Math.min(Math.floor(delayMs * 1.5), 5000);
    }

    if (prediction.status !== "succeeded") {
      const detail = prediction?.error || `Prediction did not succeed: ${prediction.status}`;
      throw new Error(detail);
    }

    const output = prediction.output || null;
    const fillablePdf = output?.fillable_pdf;
    if (!fillablePdf) {
      throw new Error("Replicate output missing fillable_pdf");
    }

    return {
      id: prediction.id,
      status: prediction.status,
      replicate_route: useDeploymentRoute ? "deployment" : "version",
      deployment_owner: useDeploymentRoute ? deploymentOwner : null,
      deployment_name: useDeploymentRoute ? deploymentName : null,
      model_version: useDeploymentRoute ? null : version,
      fast_requested: fast,
      fast_sent: Boolean(input.fast),
      replicate_input_fast: Boolean(prediction?.input?.fast),
      created_at: prediction.created_at,
      started_at: prediction.started_at,
      completed_at: prediction.completed_at,
      output,
    };
  } catch (err) {
    const message = err?.message || "Unknown error";
    const status = err?.status;
    const details = {
      status,
      modelVersion: version,
    };

    if (status === 401 || status === 403) {
      throw new functions.https.HttpsError("unauthenticated", message, details);
    }

    if (status === 429) {
      throw new functions.https.HttpsError("resource-exhausted", message, details);
    }

    if (status === 400) {
      throw new functions.https.HttpsError("invalid-argument", message, details);
    }

    throw new functions.https.HttpsError("internal", message, details);
  }
  });


export const getTextToSpeechAudioData = functions.region("us-central1").https.onCall(async (data, context) => {
  const {
    text,
    voice = "alloy",
    instructions,
    model = process.env.OPENAI_TTS_MODEL || "gpt-4o-mini-tts",
    format = process.env.OPENAI_TTS_FORMAT || "mp3",
  } = data || {};

  if (!text || typeof text !== "string" || !text.trim()) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required field: text");
  }

  const cfg = functions.config?.() || {};
  const openaiCfg = cfg.openai || {};
  const apiKey = openaiCfg.key || process.env.OPENAI_API_KEY;
  const endpoint = openaiCfg.endpoint || process.env.OPENAI_TTS_ENDPOINT || "https://api.openai.com/v1/audio/speech";

  if (!apiKey) {
    throw new functions.https.HttpsError("failed-precondition", "Server is missing OpenAI API key");
  }

  try {
    const res = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model,
        voice,
        input: text,
        instructions,
        responseFormat: format,
      }),
    });

    if (!res.ok) {
      const message = `OpenAI TTS error: ${res.status}`;
      throw new functions.https.HttpsError("internal", message);
    }

    const arrayBuffer = await res.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    return { audioBase64: buffer.toString("base64"), format };
  } catch (err) {
    const message = err?.message || "Unknown error";
    throw new functions.https.HttpsError("internal", message);
  }
});



// =============================================================================
// Referral mechanic — `claimReferral`
// -----------------------------------------------------------------------------
// Both the sender and the recipient get 30 days of Pro on grant. Idempotent:
// if the recipient calls again, we keep the same `expiresAt`. The sender's
// grant is queued; we apply it on first paid sub OR after 7 days of recipient
// retention (whichever comes first) — that retention check is done by a
// scheduled function `applyPendingReferralGrants` (below).
//
// Schema:
//   /users/{uid}.entitlements.proExpiresAt    -- timestamp
//   /users/{uid}.entitlements.referralBy      -- referrer uid
//   /referrals/{referrerUid}/queue/{recipientUid}.{createdAt,...}
// =============================================================================

import admin from "firebase-admin";
if (!admin.apps.length) admin.initializeApp();

const PRO_PLAN_DURATIONS = {
  "30d": 30 * 24 * 60 * 60 * 1000,
  "7d":  7  * 24 * 60 * 60 * 1000,
};

export const claimReferral = functions
  .region("us-central1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be signed in");
    }
    const recipientUid = context.auth.uid;
    const referrerId = (data?.referrerId || "").toString();
    const planCode = (data?.plan || "30d").toString();

    if (!referrerId || referrerId === recipientUid) {
      throw new functions.https.HttpsError("invalid-argument", "Bad referrerId");
    }
    const durationMs = PRO_PLAN_DURATIONS[planCode];
    if (!durationMs) {
      throw new functions.https.HttpsError("invalid-argument", "Unknown plan");
    }

    const db = admin.firestore();
    const recipientRef = db.collection("users").doc(recipientUid);
    const referrerRef  = db.collection("users").doc(referrerId);
    const queueRef     = db.collection("referrals").doc(referrerId)
                          .collection("queue").doc(recipientUid);

    const now = Date.now();

    await db.runTransaction(async (tx) => {
      const recipient = await tx.get(recipientRef);
      const existingExp = recipient.data()?.entitlements?.proExpiresAt?.toMillis?.() || 0;

      // Recipient grant — extend if already Pro, else set fresh.
      const recipientNewExp = Math.max(existingExp, now) + durationMs;
      tx.set(recipientRef, {
        entitlements: {
          proExpiresAt: admin.firestore.Timestamp.fromMillis(recipientNewExp),
          referralBy: referrerId,
        },
      }, { merge: true });

      // Queue the referrer grant — applied later by applyPendingReferralGrants.
      tx.set(queueRef, {
        recipientUid,
        plan: planCode,
        createdAt: admin.firestore.Timestamp.fromMillis(now),
        appliedAt: null,
        durationMs,
      });
    });

    return { ok: true };
  });

// Daily scheduled applier — grants referrer Pro once recipient retains 7+ days
// or has an active sub.
export const applyPendingReferralGrants = functions
  .region("us-central1")
  .pubsub.schedule("every 1 hours").onRun(async () => {
    const db = admin.firestore();
    const cutoff = admin.firestore.Timestamp.fromMillis(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const snap = await db.collectionGroup("queue")
      .where("appliedAt", "==", null)
      .where("createdAt", "<=", cutoff)
      .limit(200).get();

    const writes = [];
    for (const doc of snap.docs) {
      const referrerUid = doc.ref.parent.parent.id;
      const data = doc.data();
      const recipient = await db.collection("users").doc(data.recipientUid).get();
      if (!recipient.exists) { writes.push(doc.ref.delete()); continue; }
      const referrer  = db.collection("users").doc(referrerUid);
      const referrerSnap = await referrer.get();
      const existing = referrerSnap.data()?.entitlements?.proExpiresAt?.toMillis?.() || 0;
      const newExp = Math.max(existing, Date.now()) + data.durationMs;
      writes.push(referrer.set({
        entitlements: { proExpiresAt: admin.firestore.Timestamp.fromMillis(newExp) },
      }, { merge: true }));
      writes.push(doc.ref.update({ appliedAt: admin.firestore.FieldValue.serverTimestamp() }));
    }
    await Promise.all(writes);
    return null;
  });

// =============================================================================
// Email-to-fill — `inboundEmail`
// -----------------------------------------------------------------------------
// HTTP webhook for SendGrid Inbound Parse. SendGrid forwards messages sent to
// `fill@formbuddy.app` here as multipart form-data; we extract the first PDF
// attachment, hand it to `processDocument` so it gets a fillable widget tree,
// then email the result back to the sender.
//
// Setup:
//   1. Verify the `formbuddy.app` MX record points at SendGrid.
//   2. In SendGrid → Settings → Inbound Parse, add the URL of this function.
//   3. Set `SENDGRID_API_KEY` and `SENDGRID_FROM_EMAIL` in functions config.
// =============================================================================

import express from "express";
import multer from "multer";

const inboundApp = express();
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 25 * 1024 * 1024 } });

inboundApp.post("/", upload.any(), async (req, res) => {
  try {
    const senderEmail = (req.body?.from || "").toString().trim();
    if (!senderEmail) return res.status(400).send("Missing 'from'");

    const pdf = (req.files || []).find((f) => f.mimetype === "application/pdf");
    if (!pdf) return res.status(204).send(); // ignore mails without a PDF

    // Reuse the same Replicate pipeline as the in-app flow.
    const base64 = pdf.buffer.toString("base64");
    const result = await invokeProcessDocumentInline({ base64Document: base64, mimeType: "application/pdf" });
    const fillableUrl = result?.output?.fillable_pdf;
    if (!fillableUrl) return res.status(502).send("Replicate did not return a fillable PDF");

    // Download the rendered fillable PDF and forward it back to the sender.
    const resp = await fetch(fillableUrl);
    const buf = Buffer.from(await resp.arrayBuffer());

    const sendgridKey = process.env.SENDGRID_API_KEY;
    const fromEmail = process.env.SENDGRID_FROM_EMAIL || "fill@formbuddy.app";
    if (!sendgridKey) return res.status(503).send("Missing SENDGRID_API_KEY");

    const mailRes = await fetch("https://api.sendgrid.com/v3/mail/send", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${sendgridKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        personalizations: [{ to: [{ email: senderEmail }] }],
        from: { email: fromEmail, name: "FormBuddy" },
        subject: "Your filled form is ready",
        content: [{ type: "text/plain",
          value: "We turned your scanned form into a fillable PDF. Open the attachment, fill what's needed, and forward to the recipient.\n\n— FormBuddy",
        }],
        attachments: [{
          content: buf.toString("base64"),
          filename: pdf.originalname || "filled.pdf",
          type: "application/pdf",
          disposition: "attachment",
        }],
      }),
    });

    if (!mailRes.ok) {
      const text = await mailRes.text();
      console.error("SendGrid send failed", text);
      return res.status(502).send("Mail send failed");
    }
    return res.status(200).send("OK");
  } catch (err) {
    console.error("inboundEmail failure", err);
    return res.status(500).send("error");
  }
});

export const inboundEmail = functions
  .region("us-central1")
  .runWith({ timeoutSeconds: 540, memory: "1GB" })
  .https.onRequest(inboundApp);

// Internal helper that runs the same Replicate flow as the public callable
// without going through the auth-required onCall wrapper.
async function invokeProcessDocumentInline({ base64Document, mimeType }) {
  const cfg = functions.config?.() || {};
  const replicateCfg = cfg.replicate || {};
  const token = replicateCfg.token || process.env.REPLICATE_API_TOKEN;
  const version = replicateCfg.common_forms_version || process.env.REPLICATE_COMMON_FORMS_VERSION;
  if (!token || !version) throw new Error("Replicate config missing");

  const startRes = await fetch("https://api.replicate.com/v1/predictions", {
    method: "POST",
    headers: { Authorization: `Token ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      version,
      input: { pdf: `data:${mimeType};base64,${base64Document}`, return_fillable_pdf: true },
    }),
  });
  if (!startRes.ok) throw new Error(`Replicate ${startRes.status}`);
  const start = await startRes.json();
  let attempt = 0;
  let outcome = start;
  while (outcome.status === "starting" || outcome.status === "processing") {
    await new Promise((r) => setTimeout(r, 2_000));
    if (++attempt > 270) throw new Error("Replicate poll timeout");
    const res = await fetch(outcome.urls.get, { headers: { Authorization: `Token ${token}` } });
    outcome = await res.json();
  }
  return outcome;
}

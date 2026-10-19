// TutorLink Mobile API — one Supabase Edge Function that the Android app calls for all its data.
// Every call carries the user's access token. Queries run as that user, so RLS still applies.
// The service-role client is only used where the caller has no rights yet (lecturer sign-up,
// creating a tutor on approval) or for counts across tutors a lecturer may see.
import { createClient, SupabaseClient } from "npm:@supabase/supabase-js@2.45.4";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const CAMPUS = "rosebank.ac.za";
const STARTER_PASSWORD = "123456"; // test password for approved tutors (see README)
const PASSWORD_PLACEHOLDER = "managed by Supabase Auth";

type Role = "tutor" | "lecturer" | "admin";

interface Account {
  id: string;
  role: Role;
  name: string;
  email: string;
  student_number: string | null;
  approved_at: string | null;
  is_paid: boolean;
}

interface Ctx {
  db: SupabaseClient; // runs as the signed-in user (RLS applies)
  admin: SupabaseClient; // service role, used sparingly
  userId: string;
  email: string;
  account: Account | null;
  params: Record<string, string>;
  query: URLSearchParams;
  body: Record<string, unknown>;
}

class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

function fail(status: number, message: string): never {
  throw new HttpError(status, message);
}

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, OPTIONS",
};

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { ...cors, "Content-Type": "application/json" } });

// Unwraps a Supabase result, turning database errors into a safe 500.
async function run<T>(p: PromiseLike<{ data: T; error: unknown }>): Promise<T> {
  const { data, error } = await p;
  if (error) {
    console.error(error);
    fail(500, "Something went wrong on the server. Try again.");
  }
  return data;
}

// Today in South Africa (UTC+2), as YYYY-MM-DD.
const today = () => new Date(Date.now() + 2 * 3600_000).toISOString().slice(0, 10);

/* ---------- input checks ---------- */

function text(body: Record<string, unknown>, key: string, label: string, max: number): string {
  const v = body[key];
  if (typeof v !== "string" || !v.trim()) fail(400, `${label} is required`);
  const s = (v as string).trim();
  if (s.length > max) fail(400, `${label} must be ${max} characters or fewer`);
  return s;
}

function dateField(body: Record<string, unknown>, key: string): string {
  const v = body[key];
  if (typeof v !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(v) || isNaN(Date.parse(v))) {
    fail(400, "Enter a valid date (YYYY-MM-DD)");
  }
  if ((v as string) > today()) fail(400, "The session date can't be in the future");
  return v as string;
}

function studentCount(body: Record<string, unknown>): number {
  const v = body["studentCount"];
  if (typeof v !== "number" || !Number.isInteger(v) || v < 1 || v > 60) {
    fail(400, "Student count must be a whole number from 1 to 60");
  }
  return v as number;
}

async function moduleCodes(ctx: Ctx, key = "modules"): Promise<string[]> {
  const v = ctx.body[key];
  if (!Array.isArray(v) || v.length === 0 || v.some((c) => typeof c !== "string")) {
    fail(400, "Pick at least one module");
  }
  const codes = [...new Set(v as string[])];
  const found = await run(ctx.db.from("modules").select("code").in("code", codes));
  if (found.length !== codes.length) fail(400, "One or more modules don't exist");
  return codes;
}

/* ---------- shared lookups ---------- */

async function programme(db: SupabaseClient) {
  const rows = await run(db.from("settings").select("key, value"));
  const map: Record<string, string> = {};
  for (const r of rows) map[r.key] = r.value;
  return {
    ratePerSession: Number(map["rate_per_session"] ?? 150),
    minMark: Number(map["min_mark"] ?? 75),
    all: map,
  };
}

async function modulesOf(db: SupabaseClient, accountId: string) {
  const rows = await run(
    db.from("account_modules").select("module_code, modules(name)").eq("account_id", accountId),
  );
  // deno-lint-ignore no-explicit-any
  return rows.map((r: any) => ({ code: r.module_code, name: r.modules?.name ?? r.module_code }));
}

// deno-lint-ignore no-explicit-any
const toMessage = (m: any, tutorName?: string) => ({
  id: m.id,
  tutorId: m.tutor_id,
  ...(tutorName !== undefined ? { tutorName } : {}),
  senderId: m.sender_id,
  senderRole: m.sender_role,
  senderName: m.sender_name,
  topic: m.topic,
  body: m.body,
  sentAt: m.sent_at,
  readByTutor: m.read_by_tutor,
});

// deno-lint-ignore no-explicit-any
const toSession = (s: any) => ({
  id: s.id,
  tutorId: s.tutor_id,
  moduleCode: s.module_code,
  date: s.session_date,
  studentCount: s.student_count,
  confirmed: s.confirmed,
  paid: s.paid,
  loggedBy: s.logged_by,
});

// deno-lint-ignore no-explicit-any
const toRegister = (d: any) => ({
  id: d.id,
  moduleCode: d.module_code,
  fileName: d.file_name,
  filePath: d.file_url,
  uploadedAt: d.uploaded_at,
});

// deno-lint-ignore no-explicit-any
function flag(applied: string[], marks: any[], minMark: number): "pass" | "fail" | null {
  if (!marks.length) return null;
  for (const code of applied) {
    const m = marks.find((r) => r.module_code === code);
    if (!m || m.mark < minMark) return "fail";
  }
  return "pass";
}

async function tutorOr404(ctx: Ctx, tutorId: string) {
  const t = await run(
    ctx.admin.from("accounts").select("id, name, email, role").eq("id", tutorId).eq("role", "tutor").maybeSingle(),
  );
  if (!t) fail(404, "Tutor not found");
  return t as { id: string; name: string; email: string };
}

async function lecturerMayReach(ctx: Ctx, tutorId: string) {
  const ok = await run(ctx.db.rpc("lecturer_can_see", { tutor: tutorId }));
  if (!ok) fail(403, "You can only message tutors on the modules you teach");
}

async function unreadFor(db: SupabaseClient, tutorId: string) {
  const { count, error } = await db.from("messages").select("id", { count: "exact", head: true })
    .eq("tutor_id", tutorId).eq("read_by_tutor", false);
  if (error) fail(500, "Something went wrong on the server. Try again.");
  return count ?? 0;
}

const shortId = () => crypto.randomUUID().replace(/-/g, "").slice(0, 10);

/* ---------- handlers: everyone ---------- */

async function getMe(ctx: Ctx) {
  const a = ctx.account!;
  const settings = await getSettings(ctx);
  return {
    account: {
      id: a.id,
      role: a.role,
      name: a.name,
      email: a.email,
      studentNumber: a.student_number,
      approvedAt: a.approved_at,
      paid: a.is_paid,
    },
    modules: await modulesOf(ctx.db, a.id),
    settings,
    unread: a.role === "tutor" ? await unreadFor(ctx.db, a.id) : 0,
  };
}

async function getSettings(ctx: Ctx) {
  const s = await run(
    ctx.db.from("user_settings").select("haptics, reduce_motion, message_previews")
      .eq("account_id", ctx.account!.id).maybeSingle(),
  );
  return {
    haptics: s?.haptics ?? true,
    reduceMotion: s?.reduce_motion ?? false,
    messagePreviews: s?.message_previews ?? true,
  };
}

async function putSettings(ctx: Ctx) {
  const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
  const fields: [string, string][] = [["haptics", "haptics"], ["reduceMotion", "reduce_motion"], [
    "messagePreviews",
    "message_previews",
  ]];
  for (const [k, col] of fields) {
    if (k in ctx.body) {
      if (typeof ctx.body[k] !== "boolean") fail(400, `${k} must be true or false`);
      patch[col] = ctx.body[k];
    }
  }
  if (Object.keys(patch).length === 1) fail(400, "Nothing to update");
  await run(ctx.db.from("user_settings").upsert({ account_id: ctx.account!.id, ...patch }));
  return getSettings(ctx);
}

async function markRead(ctx: Ctx) {
  const rows = await run(
    ctx.db.from("messages").update({ read_by_tutor: true })
      .eq("id", ctx.params.id).eq("tutor_id", ctx.account!.id).select("id"),
  );
  if (!rows.length) fail(404, "Message not found");
  return { ok: true, unread: await unreadFor(ctx.db, ctx.account!.id) };
}

/* ---------- handlers: lecturer ---------- */

// Public: creates the lecturer's login and account in one go. Supabase's own sign-up
// rejects campus addresses, so the login is made here with the service role.
async function lecturerRegister(ctx: Ctx) {
  const name = text(ctx.body, "name", "Your full name", 80);
  const email = text(ctx.body, "email", "Email", 120).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(email)) fail(400, "Enter a valid email address");
  const domain = email.split("@")[1] ?? "";
  if (domain !== CAMPUS && !domain.endsWith("." + CAMPUS)) fail(400, "Use your rosebank.ac.za campus address");
  const password = ctx.body.password;
  if (typeof password !== "string" || password.length < 6) fail(400, "Password needs at least 6 characters");
  if (password.length > 72) fail(400, "Password must be 72 characters or fewer");
  const codes = await moduleCodes({ ...ctx, db: ctx.admin });

  const taken = await run(ctx.admin.from("accounts").select("id").ilike("email", email).maybeSingle());
  if (taken) fail(409, "An account already uses that email");

  const { data: created, error } = await ctx.admin.auth.admin.createUser({ email, password, email_confirm: true });
  if (error || !created.user) {
    if (error && /already/i.test(error.message)) fail(409, "An account already uses that email");
    console.error(error);
    fail(500, "Could not create your account. Try again.");
  }
  const id = "lect_" + shortId();
  try {
    await run(ctx.admin.from("accounts").insert({
      id,
      role: "lecturer",
      name,
      email,
      password: PASSWORD_PLACEHOLDER,
    }));
    await run(ctx.admin.from("account_users").insert({ user_id: created!.user!.id, account_id: id }));
    await run(ctx.admin.from("account_modules").insert(codes.map((c) => ({ account_id: id, module_code: c }))));
  } catch (e) {
    await ctx.admin.auth.admin.deleteUser(created!.user!.id);
    await ctx.admin.from("accounts").delete().eq("id", id);
    throw e;
  }
  return { ok: true, account: { id, role: "lecturer", name, email } };
}

async function lecturerModules(ctx: Ctx) {
  const codes = await moduleCodes(ctx);
  const me = ctx.account!.id;
  await run(ctx.db.from("account_modules").delete().eq("account_id", me));
  await run(ctx.db.from("account_modules").insert(codes.map((c) => ({ account_id: me, module_code: c }))));
  return { modules: await modulesOf(ctx.db, me) };
}

async function lecturerTutors(ctx: Ctx) {
  const me = ctx.account!.id;
  const mods = await modulesOf(ctx.db, me);
  // RLS only returns tutors who share a module with this lecturer.
  const tutors = await run(
    ctx.db.from("accounts").select("id, name, email, account_modules(module_code)").eq("role", "tutor").order("name"),
  );
  // deno-lint-ignore no-explicit-any
  const ids = tutors.map((t: any) => t.id);
  const msgs = ids.length
    ? await run(ctx.db.from("messages").select("*").in("tutor_id", ids).order("sent_at", { ascending: false }))
    : [];
  let sessions = 0;
  if (ids.length) {
    const { count } = await ctx.admin.from("sessions").select("id", { count: "exact", head: true }).in("tutor_id", ids);
    sessions = count ?? 0;
  }
  // deno-lint-ignore no-explicit-any
  const list = tutors.map((t: any) => {
    // deno-lint-ignore no-explicit-any
    const thread = msgs.filter((m: any) => m.tutor_id === t.id);
    return {
      id: t.id,
      name: t.name,
      email: t.email,
      // deno-lint-ignore no-explicit-any
      modules: t.account_modules.map((m: any) => m.module_code),
      lastMessage: thread[0] ? { topic: thread[0].topic, sentAt: thread[0].sent_at } : null,
      // deno-lint-ignore no-explicit-any
      unread: thread.some((m: any) => !m.read_by_tutor),
    };
  });
  return {
    modules: mods,
    counts: { tutors: list.length, modules: mods.length, sessions },
    // deno-lint-ignore no-explicit-any
    hasSentMessage: msgs.some((m: any) => m.sender_id === me),
    tutors: list,
  };
}

// The lecturer's Messages tab lists what they sent, newest first (as in the demo).
async function lecturerThreads(ctx: Ctx) {
  const msgs = await run(
    ctx.db.from("messages").select("*, tutor:accounts!messages_tutor_id_fkey(name)")
      .eq("sender_id", ctx.account!.id).order("sent_at", { ascending: false }),
  );
  // deno-lint-ignore no-explicit-any
  return { messages: msgs.map((m: any) => toMessage(m, m.tutor?.name ?? "")) };
}

/* ---------- handlers: tutor ---------- */

async function tutorSchedule(ctx: Ctx) {
  const me = ctx.account!.id;
  const [schedule, sessions, prog, unread, modules] = await Promise.all([
    run(ctx.db.from("schedule").select("*").eq("tutor_id", me).order("sort_order")),
    run(ctx.db.from("sessions").select("*").eq("tutor_id", me).order("session_date", { ascending: false })),
    programme(ctx.db),
    unreadFor(ctx.db, me),
    modulesOf(ctx.db, me),
  ]);
  // deno-lint-ignore no-explicit-any
  const confirmed = sessions.filter((s: any) => s.confirmed).length;
  return {
    modules,
    unread,
    // deno-lint-ignore no-explicit-any
    upcoming: schedule.map((s: any) => ({
      id: s.id,
      moduleCode: s.module_code,
      day: s.day_label,
      time: s.time_label,
      venue: s.venue,
      studentCount: s.student_count,
      live: s.is_live,
      marked: s.marked,
    })),
    month: { sessions: sessions.length, confirmed, earned: confirmed * prog.ratePerSession },
    // deno-lint-ignore no-explicit-any
    pending: sessions.filter((s: any) => !s.confirmed).map(toSession),
  };
}

async function tutorMark(ctx: Ctx) {
  const me = ctx.account!.id;
  const s = await run(ctx.db.from("schedule").select("*").eq("id", ctx.params.id).eq("tutor_id", me).maybeSingle());
  if (!s) fail(404, "Session not found");
  if (!s.is_live) fail(400, "Only today's session can be marked");
  if (s.marked) fail(409, "This session is already logged");
  await run(ctx.db.from("schedule").update({ marked: true }).eq("id", s.id));
  const row = await run(
    ctx.db.from("sessions").insert({
      tutor_id: me,
      module_code: s.module_code,
      session_date: today(),
      student_count: Math.max(1, s.student_count),
      confirmed: false,
      paid: false,
      logged_by: "tutor",
    }).select().single(),
  );
  return { session: toSession(row) };
}

async function tutorSessions(ctx: Ctx) {
  const rows = await run(
    ctx.db.from("sessions").select("*").eq("tutor_id", ctx.account!.id).order("session_date", { ascending: false }),
  );
  return { sessions: rows.map(toSession) };
}

async function tutorLogSession(ctx: Ctx) {
  const me = ctx.account!.id;
  const date = dateField(ctx.body, "date");
  const moduleCode = text(ctx.body, "moduleCode", "Module", 20);
  const count = studentCount(ctx.body);
  const mine = (await modulesOf(ctx.db, me)).map((m) => m.code);
  if (!mine.includes(moduleCode)) fail(400, "You can only log sessions for your own modules");
  const row = await run(
    ctx.db.from("sessions").insert({
      tutor_id: me,
      module_code: moduleCode,
      session_date: date,
      student_count: count,
      confirmed: false,
      paid: false,
      logged_by: "tutor",
    }).select().single(),
  );
  return { session: toSession(row) };
}

async function tutorRegisters(ctx: Ctx) {
  const rows = await run(
    ctx.db.from("attendance_docs").select("*").eq("tutor_id", ctx.account!.id).order("uploaded_at", { ascending: false }),
  );
  return { registers: rows.map(toRegister) };
}

async function tutorSaveRegister(ctx: Ctx) {
  const me = ctx.account!.id;
  const moduleCode = text(ctx.body, "moduleCode", "Module", 20);
  const fileName = text(ctx.body, "fileName", "File name", 200);
  const filePath = typeof ctx.body.filePath === "string" ? ctx.body.filePath : null;
  if (filePath && !filePath.startsWith(me + "/")) fail(400, "Upload the file to your own folder first");
  const mine = (await modulesOf(ctx.db, me)).map((m) => m.code);
  if (!mine.includes(moduleCode)) fail(400, "You can only upload registers for your own modules");
  const row = await run(
    ctx.db.from("attendance_docs").insert({
      tutor_id: me,
      module_code: moduleCode,
      file_name: fileName,
      file_url: filePath,
      uploaded_at: today(),
    }).select().single(),
  );
  return { register: toRegister(row) };
}

async function tutorPay(ctx: Ctx) {
  const me = ctx.account!.id;
  const [sessions, prog] = await Promise.all([
    run(ctx.db.from("sessions").select("*").eq("tutor_id", me).order("session_date", { ascending: false })),
    programme(ctx.db),
  ]);
  const rate = prog.ratePerSession;
  // deno-lint-ignore no-explicit-any
  const confirmed = sessions.filter((s: any) => s.confirmed);
  const pending = sessions.length - confirmed.length;
  return {
    rate,
    approved: confirmed.length * rate,
    confirmedCount: confirmed.length,
    pendingAmount: pending * rate,
    projected: sessions.length * rate,
    paid: ctx.account!.is_paid,
    // deno-lint-ignore no-explicit-any
    lines: confirmed.map((s: any) => ({ ...toSession(s), amount: rate })),
  };
}

async function tutorMessages(ctx: Ctx) {
  const rows = await run(
    ctx.db.from("messages").select("*").eq("tutor_id", ctx.account!.id).order("sent_at", { ascending: false }),
  );
  return { messages: rows.map((m) => toMessage(m)) };
}

/* ---------- handlers: lecturer + admin ---------- */

async function getThread(ctx: Ctx) {
  const tutor = await tutorOr404(ctx, ctx.params.tutorId);
  if (ctx.account!.role === "lecturer") await lecturerMayReach(ctx, tutor.id);
  const rows = await run(
    ctx.db.from("messages").select("*").eq("tutor_id", tutor.id).order("sent_at", { ascending: false }),
  );
  return {
    tutor: { id: tutor.id, name: tutor.name, modules: (await modulesOf(ctx.admin, tutor.id)).map((m) => m.code) },
    messages: rows.map((m) => toMessage(m)),
  };
}

async function postMessage(ctx: Ctx) {
  const a = ctx.account!;
  const tutorId = text(ctx.body, "tutorId", "Tutor", 64);
  const topic = text(ctx.body, "topic", "Topic", 120);
  const body = text(ctx.body, "body", "Message", 2000);
  await tutorOr404(ctx, tutorId);
  if (a.role === "lecturer") await lecturerMayReach(ctx, tutorId);
  const row = await run(
    ctx.db.from("messages").insert({
      tutor_id: tutorId,
      sender_id: a.id,
      sender_role: a.role,
      sender_name: a.name,
      topic,
      body,
      read_by_tutor: false,
    }).select().single(),
  );
  return { message: toMessage(row) };
}

/* ---------- handlers: admin ---------- */

async function adminOverview(ctx: Ctx) {
  const [apps, tutors, sessions, prog] = await Promise.all([
    run(ctx.db.from("applications").select("status")),
    run(ctx.db.from("accounts").select("id").eq("role", "tutor")),
    run(ctx.db.from("sessions").select("confirmed, paid")),
    programme(ctx.db),
  ]);
  // deno-lint-ignore no-explicit-any
  const unpaid = sessions.filter((s: any) => s.confirmed && !s.paid).length;
  return {
    // deno-lint-ignore no-explicit-any
    openApplications: apps.filter((a: any) => a.status === "pending" || a.status === "under_review").length,
    activeTutors: tutors.length,
    outstanding: unpaid * prog.ratePerSession,
    // deno-lint-ignore no-explicit-any
    awaitingConfirmation: sessions.filter((s: any) => !s.confirmed).length,
    rate: prog.ratePerSession,
  };
}

const STATUSES = ["pending", "under_review", "approved", "rejected"];

async function adminApplications(ctx: Ctx) {
  const status = ctx.query.get("status") ?? "all";
  if (status !== "all" && !STATUSES.includes(status)) fail(400, "Unknown status filter");
  let q = ctx.db.from("applications")
    .select("*, application_modules(module_code), report_modules(module_code, mark)")
    .order("submitted_at", { ascending: false });
  if (status !== "all") q = q.eq("status", status);
  const [rows, prog] = await Promise.all([run(q), programme(ctx.db)]);
  return {
    // deno-lint-ignore no-explicit-any
    applications: rows.map((a: any) => {
      // deno-lint-ignore no-explicit-any
      const applied = a.application_modules.map((m: any) => m.module_code);
      return {
        id: a.id,
        name: a.name,
        email: a.email,
        studentNumber: a.student_number,
        status: a.status,
        submittedAt: a.submitted_at,
        appliedModules: applied,
        flag: flag(applied, a.report_modules, prog.minMark),
      };
    }),
  };
}

async function loadApplication(ctx: Ctx) {
  const a = await run(
    ctx.db.from("applications")
      .select("*, application_modules(module_code), report_modules(module_code, mark, result, modules(name))")
      .eq("id", ctx.params.id).maybeSingle(),
  );
  if (!a) fail(404, "Application not found");
  return a;
}

async function adminApplication(ctx: Ctx) {
  const [a, prog] = await Promise.all([loadApplication(ctx), programme(ctx.db)]);
  // deno-lint-ignore no-explicit-any
  const applied: string[] = a.application_modules.map((m: any) => m.module_code);
  return {
    id: a.id,
    name: a.name,
    email: a.email,
    studentNumber: a.student_number,
    programme: a.programme,
    status: a.status,
    backgroundChecked: a.background_checked,
    responseMessage: a.response_message,
    submittedAt: a.submitted_at,
    processedAt: a.processed_at,
    appliedModules: applied,
    minMark: prog.minMark,
    flag: flag(applied, a.report_modules, prog.minMark),
    // deno-lint-ignore no-explicit-any
    marks: a.report_modules.map((m: any) => ({
      code: m.module_code,
      name: m.modules?.name ?? m.module_code,
      mark: m.mark,
      result: m.result,
      appliedFor: applied.includes(m.module_code),
    })),
  };
}

async function adminBackgroundCheck(ctx: Ctx) {
  const a = await loadApplication(ctx);
  const patch: Record<string, unknown> = { background_checked: true };
  if (a.status === "pending") patch.status = "under_review";
  await run(ctx.db.from("applications").update(patch).eq("id", a.id));
  return adminApplication(ctx);
}

async function adminDecision(ctx: Ctx) {
  const status = ctx.body.status;
  if (status !== "approved" && status !== "rejected") fail(400, "Status must be approved or rejected");
  const message = text(ctx.body, "message", "A response", 2000);
  const a = await loadApplication(ctx);
  if (a.status === "approved" || a.status === "rejected") fail(409, "This application has already been decided");

  let tutor: { id: string; name: string; email: string } | null = null;
  if (status === "approved") {
    // The student keeps their own email: approving upgrades their login to a tutor account.
    const email = String(a.email).toLowerCase();
    const taken = await run(ctx.admin.from("accounts").select("id").ilike("email", email).maybeSingle());
    if (taken) fail(409, "An account already uses " + email);

    const link = await run(ctx.admin.from("application_users").select("user_id").eq("application_id", a.id).maybeSingle());
    let userId: string;
    let createdHere = false;
    if (link) {
      userId = link.user_id;
    } else {
      // Applied on the website without an app login: make one (test password, see README).
      const { data: created, error } = await ctx.admin.auth.admin.createUser({
        email,
        password: STARTER_PASSWORD,
        email_confirm: true,
      });
      if (error || !created.user) {
        console.error(error);
        fail(500, "Could not create the tutor's login. Try again.");
      }
      userId = created!.user!.id;
      createdHere = true;
    }
    const id = "tutor_" + shortId();
    try {
      await run(ctx.admin.from("accounts").insert({
        id,
        role: "tutor",
        name: a.name,
        email,
        password: PASSWORD_PLACEHOLDER,
        student_number: a.student_number,
        approved_at: today(),
      }));
      await run(ctx.admin.from("account_users").insert({ user_id: userId, account_id: id }));
      // deno-lint-ignore no-explicit-any
      const mods = a.application_modules.map((m: any) => ({ account_id: id, module_code: m.module_code }));
      if (mods.length) await run(ctx.admin.from("account_modules").insert(mods));
      // The welcome notice lands in their thread the moment the account exists.
      await run(ctx.admin.from("messages").insert({
        tutor_id: id,
        sender_id: ctx.account!.id,
        sender_role: "admin",
        sender_name: ctx.account!.name,
        topic: "Welcome to the tutoring programme",
        body: message,
        read_by_tutor: false,
      }));
    } catch (e) {
      await ctx.admin.from("account_users").delete().eq("account_id", id);
      await ctx.admin.from("accounts").delete().eq("id", id);
      if (createdHere) await ctx.admin.auth.admin.deleteUser(userId);
      throw e;
    }
    tutor = { id, name: a.name, email };
  }

  await run(ctx.db.from("applications").update({
    status,
    response_message: message,
    processed_at: today(),
  }).eq("id", a.id));
  return { application: await adminApplication(ctx), tutor };
}

async function adminTutors(ctx: Ctx) {
  const [tutors, sessions, prog] = await Promise.all([
    run(ctx.db.from("accounts").select("id, name, email, is_paid, account_modules(module_code)").eq("role", "tutor").order("name")),
    run(ctx.db.from("sessions").select("tutor_id, confirmed, paid")),
    programme(ctx.db),
  ]);
  return {
    // deno-lint-ignore no-explicit-any
    tutors: tutors.map((t: any) => ({
      id: t.id,
      name: t.name,
      email: t.email,
      paid: t.is_paid,
      // deno-lint-ignore no-explicit-any
      modules: t.account_modules.map((m: any) => m.module_code),
      // deno-lint-ignore no-explicit-any
      owed: sessions.filter((s: any) => s.tutor_id === t.id && s.confirmed && !s.paid).length * prog.ratePerSession,
    })),
  };
}

async function adminTutor(ctx: Ctx) {
  const id = ctx.params.id;
  const t = await run(ctx.db.from("accounts").select("*").eq("id", id).eq("role", "tutor").maybeSingle());
  if (!t) fail(404, "Tutor not found");
  const [modules, sessions, registers, prog] = await Promise.all([
    modulesOf(ctx.db, id),
    run(ctx.db.from("sessions").select("*").eq("tutor_id", id).order("session_date", { ascending: false })),
    run(ctx.db.from("attendance_docs").select("*").eq("tutor_id", id).order("uploaded_at", { ascending: false })),
    programme(ctx.db),
  ]);
  // deno-lint-ignore no-explicit-any
  const owedCount = sessions.filter((s: any) => s.confirmed && !s.paid).length;
  return {
    tutor: {
      id: t.id,
      name: t.name,
      email: t.email,
      studentNumber: t.student_number,
      approvedAt: t.approved_at,
      paid: t.is_paid,
    },
    modules,
    sessions: sessions.map(toSession),
    registers: registers.map(toRegister),
    pay: {
      rate: prog.ratePerSession,
      sessions: sessions.length,
      confirmedUnpaid: owedCount,
      // deno-lint-ignore no-explicit-any
      pending: sessions.filter((s: any) => !s.confirmed).length,
      owed: owedCount * prog.ratePerSession,
    },
  };
}

async function adminConfirm(ctx: Ctx) {
  const s = await run(ctx.db.from("sessions").select("*").eq("id", ctx.params.id).maybeSingle());
  if (!s) fail(404, "Session not found");
  if (s.confirmed) fail(409, "This session is already confirmed");
  const row = await run(ctx.db.from("sessions").update({ confirmed: true }).eq("id", s.id).select().single());
  await run(ctx.db.from("accounts").update({ is_paid: false }).eq("id", s.tutor_id));
  return { session: toSession(row) };
}

async function adminAddSession(ctx: Ctx) {
  const id = ctx.params.id;
  await tutorOr404(ctx, id);
  const date = dateField(ctx.body, "date");
  const moduleCode = text(ctx.body, "moduleCode", "Module", 20);
  const count = studentCount(ctx.body);
  const mods = (await modulesOf(ctx.db, id)).map((m) => m.code);
  if (!mods.includes(moduleCode)) fail(400, "That tutor doesn't tutor this module");
  const row = await run(
    ctx.db.from("sessions").insert({
      tutor_id: id,
      module_code: moduleCode,
      session_date: date,
      student_count: count,
      confirmed: true,
      paid: false,
      logged_by: "admin",
    }).select().single(),
  );
  await run(ctx.db.from("accounts").update({ is_paid: false }).eq("id", id));
  return { session: toSession(row) };
}

async function adminMarkPaid(ctx: Ctx) {
  const id = ctx.params.id;
  await tutorOr404(ctx, id);
  const rows = await run(
    ctx.db.from("sessions").update({ paid: true }).eq("tutor_id", id).eq("confirmed", true).eq("paid", false).select("id"),
  );
  await run(ctx.db.from("accounts").update({ is_paid: true }).eq("id", id));
  return { ok: true, sessionsPaid: rows.length };
}

async function adminThreads(ctx: Ctx) {
  const rows = await run(
    ctx.db.from("messages").select("*, tutor:accounts!messages_tutor_id_fkey(name)").order("sent_at", { ascending: false }),
  );
  // deno-lint-ignore no-explicit-any
  return { messages: rows.map((m: any) => toMessage(m, m.tutor?.name ?? "")) };
}

async function getProgramme(ctx: Ctx) {
  const p = await programme(ctx.db);
  return { ratePerSession: p.ratePerSession, minMark: p.minMark, other: p.all };
}

async function putProgramme(ctx: Ctx) {
  const rows: { key: string; value: string }[] = [];
  if ("ratePerSession" in ctx.body) {
    const v = ctx.body.ratePerSession;
    if (typeof v !== "number" || !(v > 0) || v > 100000) fail(400, "Enter a valid rate");
    rows.push({ key: "rate_per_session", value: String(v) });
  }
  if ("minMark" in ctx.body) {
    const v = ctx.body.minMark;
    if (typeof v !== "number" || v < 0 || v > 100) fail(400, "Minimum mark must be between 0 and 100");
    rows.push({ key: "min_mark", value: String(v) });
  }
  if (!rows.length) fail(400, "Nothing to update");
  await run(ctx.db.from("settings").upsert(rows));
  return getProgramme(ctx);
}

async function adminTemplates(ctx: Ctx) {
  const rows = await run(ctx.db.from("response_templates").select("id, label, body").order("sort_order"));
  return { templates: rows };
}

/* ---------- handlers: students applying to tutor ---------- */

// Public: a student registers with any email and a password (hashed by Supabase Auth).
async function applicantRegister(ctx: Ctx) {
  const email = text(ctx.body, "email", "Email", 120).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(email)) fail(400, "Enter a valid email address");
  const password = ctx.body.password;
  if (typeof password !== "string" || password.length < 6) fail(400, "Password needs at least 6 characters");
  if (password.length > 72) fail(400, "Password must be 72 characters or fewer");
  const { error } = await ctx.admin.auth.admin.createUser({
    email,
    password,
    email_confirm: true,
    user_metadata: { password_set: true },
  });
  if (error) {
    if (/already/i.test(error.message)) fail(409, "An account already uses that email. Sign in instead.");
    console.error(error);
    fail(500, "Could not create your account. Try again.");
  }
  return { ok: true };
}

const markResult = (m: number) => (m >= 75 ? "PD" : m >= 50 ? "P" : "F");

async function applicantStatus(ctx: Ctx) {
  const link = await run(ctx.admin.from("application_users").select("application_id").eq("user_id", ctx.userId).maybeSingle());
  if (!link) return { email: ctx.email, application: null };
  const a = await run(
    ctx.admin.from("applications").select("*, application_modules(module_code)").eq("id", link.application_id).single(),
  );
  return {
    email: ctx.email,
    application: {
      id: a.id,
      name: a.name,
      status: a.status,
      submittedAt: a.submitted_at,
      processedAt: a.processed_at,
      responseMessage: a.response_message,
      // deno-lint-ignore no-explicit-any
      appliedModules: a.application_modules.map((m: any) => m.module_code),
    },
  };
}

// The student's qualifications: the modules they want to tutor and their mark in each.
async function applicantApply(ctx: Ctx) {
  if (ctx.account) fail(409, "You already have a TutorLink account");
  const existing = await run(ctx.admin.from("application_users").select("application_id").eq("user_id", ctx.userId).maybeSingle());
  if (existing) fail(409, "You've already applied. Check your status instead.");
  const name = text(ctx.body, "name", "Your full name", 80);
  const studentNumber = text(ctx.body, "studentNumber", "Student number", 20).toUpperCase();
  if (!/^[A-Z]{0,3}\d{6,10}$/.test(studentNumber)) fail(400, "Enter a valid student number, e.g. ST10398445");
  const programme = typeof ctx.body.programme === "string" && ctx.body.programme.trim()
    ? ctx.body.programme.trim().slice(0, 120)
    : "Diploma in IT";
  const marks = ctx.body.marks;
  if (!Array.isArray(marks) || marks.length === 0) fail(400, "Add at least one module and your mark");
  const rows: { code: string; mark: number }[] = [];
  for (const m of marks as unknown[]) {
    const r = m as Record<string, unknown>;
    if (typeof r?.code !== "string" || typeof r?.mark !== "number" || !Number.isInteger(r.mark) || r.mark < 0 || r.mark > 100) {
      fail(400, "Each mark must be a whole number from 0 to 100");
    }
    rows.push({ code: r.code as string, mark: r.mark as number });
  }
  const codes = [...new Set(rows.map((r) => r.code))];
  if (codes.length !== rows.length) fail(400, "Each module can only appear once");
  const found = await run(ctx.admin.from("modules").select("code").in("code", codes));
  if (found.length !== codes.length) fail(400, "One or more modules don't exist");

  const id = "app_" + shortId();
  await run(ctx.admin.from("applications").insert({
    id,
    name,
    email: ctx.email.toLowerCase(),
    student_number: studentNumber,
    programme,
    status: "pending",
  }));
  try {
    await run(ctx.admin.from("application_modules").insert(codes.map((c) => ({ application_id: id, module_code: c }))));
    await run(ctx.admin.from("report_modules").insert(rows.map((r) => ({
      application_id: id,
      module_code: r.code,
      mark: r.mark,
      result: markResult(r.mark),
    }))));
    await run(ctx.admin.from("application_users").insert({ user_id: ctx.userId, application_id: id }));
  } catch (e) {
    await ctx.admin.from("report_modules").delete().eq("application_id", id);
    await ctx.admin.from("application_modules").delete().eq("application_id", id);
    await ctx.admin.from("applications").delete().eq("id", id);
    throw e;
  }
  return applicantStatus(ctx);
}

// Admin adds a lecturer; they sign in with Google the first time and choose a password.
async function adminAddLecturer(ctx: Ctx) {
  const name = text(ctx.body, "name", "Name", 80);
  const email = text(ctx.body, "email", "Email", 120).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(email)) fail(400, "Enter a valid email address");
  const codes = await moduleCodes(ctx);
  const taken = await run(ctx.admin.from("accounts").select("id").ilike("email", email).maybeSingle());
  if (taken) fail(409, "An account already uses that email");

  // Reuse the login if this email already signed in once (e.g. with Google), otherwise create it.
  const { data: list } = await ctx.admin.auth.admin.listUsers({ page: 1, perPage: 1000 });
  let userId = list?.users.find((u) => (u.email ?? "").toLowerCase() === email)?.id;
  let createdHere = false;
  if (!userId) {
    const { data: created, error } = await ctx.admin.auth.admin.createUser({
      email,
      password: crypto.randomUUID() + "Aa1!",
      email_confirm: true,
    });
    if (error || !created.user) {
      console.error(error);
      fail(500, "Could not create the lecturer's login. Try again.");
    }
    userId = created!.user!.id;
    createdHere = true;
  }
  const id = "lect_" + shortId();
  try {
    await run(ctx.admin.from("accounts").insert({ id, role: "lecturer", name, email, password: PASSWORD_PLACEHOLDER }));
    await run(ctx.admin.from("account_users").insert({ user_id: userId, account_id: id }));
    await run(ctx.admin.from("account_modules").insert(codes.map((c) => ({ account_id: id, module_code: c }))));
  } catch (e) {
    await ctx.admin.from("account_users").delete().eq("account_id", id);
    await ctx.admin.from("accounts").delete().eq("id", id);
    if (createdHere) await ctx.admin.auth.admin.deleteUser(userId!);
    throw e;
  }
  return { ok: true, lecturer: { id, name, email } };
}

/* ---------- routing ---------- */

type Who = Role[] | "any" | "public" | "signed-in";
type Handler = (ctx: Ctx) => Promise<unknown>;

const routes: [string, string, Who, Handler][] = [
  ["GET", "/me", "any", getMe],
  ["GET", "/settings", "any", getSettings],
  ["PUT", "/settings", "any", putSettings],
  ["PATCH", "/messages/:id/read", ["tutor"], markRead],

  ["POST", "/lecturer/register", "public", lecturerRegister],
  ["POST", "/applicant/register", "public", applicantRegister],
  ["GET", "/applicant/status", "signed-in", applicantStatus],
  ["POST", "/applicant/apply", "signed-in", applicantApply],
  ["POST", "/admin/lecturers", ["admin"], adminAddLecturer],
  ["PUT", "/lecturer/modules", ["lecturer"], lecturerModules],
  ["GET", "/lecturer/tutors", ["lecturer"], lecturerTutors],
  ["GET", "/lecturer/threads", ["lecturer"], lecturerThreads],

  ["GET", "/tutor/schedule", ["tutor"], tutorSchedule],
  ["POST", "/tutor/sessions/:id/mark", ["tutor"], tutorMark],
  ["GET", "/tutor/sessions", ["tutor"], tutorSessions],
  ["POST", "/tutor/sessions", ["tutor"], tutorLogSession],
  ["GET", "/tutor/registers", ["tutor"], tutorRegisters],
  ["POST", "/tutor/registers", ["tutor"], tutorSaveRegister],
  ["GET", "/tutor/pay", ["tutor"], tutorPay],
  ["GET", "/tutor/messages", ["tutor"], tutorMessages],

  ["GET", "/messages/thread/:tutorId", ["lecturer", "admin"], getThread],
  ["POST", "/messages", ["lecturer", "admin"], postMessage],

  ["GET", "/admin/overview", ["admin"], adminOverview],
  ["GET", "/admin/applications", ["admin"], adminApplications],
  ["GET", "/admin/applications/:id", ["admin"], adminApplication],
  ["POST", "/admin/applications/:id/background-check", ["admin"], adminBackgroundCheck],
  ["POST", "/admin/applications/:id/decision", ["admin"], adminDecision],
  ["GET", "/admin/tutors", ["admin"], adminTutors],
  ["GET", "/admin/tutors/:id", ["admin"], adminTutor],
  ["POST", "/admin/sessions/:id/confirm", ["admin"], adminConfirm],
  ["POST", "/admin/tutors/:id/sessions", ["admin"], adminAddSession],
  ["POST", "/admin/tutors/:id/mark-paid", ["admin"], adminMarkPaid],
  ["GET", "/admin/threads", ["admin"], adminThreads],
  ["GET", "/admin/programme-settings", ["admin"], getProgramme],
  ["PUT", "/admin/programme-settings", ["admin"], putProgramme],
  ["GET", "/admin/templates", ["admin"], adminTemplates],
];

async function readBody(req: Request): Promise<Record<string, unknown>> {
  if (req.method === "GET") return {};
  const raw = await req.text();
  if (!raw) return {};
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    fail(400, "Send valid JSON");
  }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) fail(400, "Send a JSON object");
  return parsed as Record<string, unknown>;
}

function match(pattern: string, path: string): Record<string, string> | null {
  const p = pattern.split("/");
  const a = path.split("/");
  if (p.length !== a.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < p.length; i++) {
    if (p[i].startsWith(":")) params[p[i].slice(1)] = decodeURIComponent(a[i]);
    else if (p[i] !== a[i]) return null;
  }
  return params;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const url = new URL(req.url);
    // Path arrives as /api/... (or /functions/v1/api/...); keep what's after "/api".
    const i = url.pathname.indexOf("/api");
    const path = (i >= 0 ? url.pathname.slice(i + 4) : url.pathname).replace(/\/+$/, "") || "/";

    let found: [Who, Handler, Record<string, string>] | null = null;
    let pathExists = false;
    for (const [method, pattern, who, handler] of routes) {
      const params = match(pattern, path);
      if (!params) continue;
      pathExists = true;
      if (method === req.method) {
        found = [who, handler, params];
        break;
      }
    }
    if (!found) return json({ error: pathExists ? "Method not allowed" : "Not found" }, pathExists ? 405 : 404);
    const [who, handler, params] = found;

    const auth = req.headers.get("Authorization") ?? "";
    const jwt = auth.replace(/^Bearer\s+/i, "");
    if (!jwt) fail(401, "Sign in first");
    const db = createClient(SUPABASE_URL, ANON_KEY, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
      auth: { persistSession: false },
    });
    const admin = createClient(SUPABASE_URL, SERVICE_KEY, { auth: { persistSession: false } });
    const body = await readBody(req);

    // Public routes (lecturer sign-up) are called with the anon key, not a user's token.
    if (who === "public") {
      const ctx: Ctx = { db, admin, userId: "", email: "", account: null, params, query: url.searchParams, body };
      return json(await handler(ctx));
    }

    const { data: userData, error: userErr } = await db.auth.getUser(jwt);
    if (userErr || !userData.user) fail(401, "Your session has expired. Sign in again.");
    const user = userData!.user!;

    const link = await run(db.from("account_users").select("account_id").eq("user_id", user.id).maybeSingle());
    const account = link
      ? await run(
        db.from("accounts").select("id, role, name, email, student_number, approved_at, is_paid")
          .eq("id", link.account_id).single(),
      ) as Account
      : null;

    // "signed-in" routes (students applying) only need a login, not a TutorLink account.
    if (who !== "signed-in") {
      if (!account) fail(403, "There's no TutorLink account for this login");
      if (who !== "any" && !who.includes(account!.role)) fail(403, "Your role can't do that");
    }

    const ctx: Ctx = { db, admin, userId: user.id, email: user.email ?? "", account, params, query: url.searchParams, body };
    return json(await handler(ctx));
  } catch (e) {
    if (e instanceof HttpError) return json({ error: e.message }, e.status);
    console.error(e);
    return json({ error: "Something went wrong on the server. Try again." }, 500);
  }
});

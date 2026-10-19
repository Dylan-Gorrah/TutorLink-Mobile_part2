-- TutorLink Mobile v2: replace the web demo's "demo open access" policies
-- (anyone with the anon key could read and write everything) with role-based ones.
-- Tutors see their own data, lecturers see tutors on their modules, admin sees everything.
-- Approved by Dylan on 19 Oct 2026. Tables and columns are untouched.

drop policy if exists "demo open access" on public.modules;
drop policy if exists "demo open access" on public.accounts;
drop policy if exists "demo open access" on public.account_modules;
drop policy if exists "demo open access" on public.applications;
drop policy if exists "demo open access" on public.application_modules;
drop policy if exists "demo open access" on public.report_modules;
drop policy if exists "demo open access" on public.sessions;
drop policy if exists "demo open access" on public.schedule;
drop policy if exists "demo open access" on public.attendance_docs;
drop policy if exists "demo open access" on public.messages;
drop policy if exists "demo open access" on public.settings;
drop policy if exists "demo open access" on public.response_templates;

-- modules: everyone signed in can read, admin manages
create policy "modules read" on public.modules for select to authenticated using (true);
create policy "modules admin" on public.modules for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- accounts
create policy "accounts read" on public.accounts for select to authenticated using (
  id = (select public.app_account_id())
  or (select public.app_role()) = 'admin'
  or ((select public.app_role()) = 'lecturer' and role = 'tutor' and public.lecturer_can_see(id))
);
create policy "accounts admin" on public.accounts for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- account_modules (tutor and lecturer modules)
create policy "account_modules read" on public.account_modules for select to authenticated using (
  account_id = (select public.app_account_id())
  or (select public.app_role()) = 'admin'
  or ((select public.app_role()) = 'lecturer' and public.lecturer_can_see(account_id))
);
create policy "account_modules lecturer insert" on public.account_modules for insert to authenticated with check (
  (account_id = (select public.app_account_id()) and (select public.app_role()) = 'lecturer')
  or (select public.app_role()) = 'admin'
);
create policy "account_modules lecturer delete" on public.account_modules for delete to authenticated using (
  (account_id = (select public.app_account_id()) and (select public.app_role()) = 'lecturer')
  or (select public.app_role()) = 'admin'
);

-- applications and their marks: admin only
create policy "applications admin" on public.applications for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');
create policy "application_modules admin" on public.application_modules for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');
create policy "report_modules admin" on public.report_modules for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- sessions: tutors read theirs and log new ones as pending; admin does the rest
create policy "sessions read" on public.sessions for select to authenticated using (
  tutor_id = (select public.app_account_id()) or (select public.app_role()) = 'admin'
);
create policy "sessions tutor log" on public.sessions for insert to authenticated with check (
  tutor_id = (select public.app_account_id())
  and logged_by = 'tutor' and confirmed = false and paid = false
);
create policy "sessions admin" on public.sessions for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- schedule: tutors read and mark their own
create policy "schedule read" on public.schedule for select to authenticated using (
  tutor_id = (select public.app_account_id()) or (select public.app_role()) = 'admin'
);
create policy "schedule tutor mark" on public.schedule for update to authenticated
  using (tutor_id = (select public.app_account_id()))
  with check (tutor_id = (select public.app_account_id()));
create policy "schedule admin" on public.schedule for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- attendance_docs: tutors read and upload their own
create policy "attendance_docs read" on public.attendance_docs for select to authenticated using (
  tutor_id = (select public.app_account_id()) or (select public.app_role()) = 'admin'
);
create policy "attendance_docs tutor upload" on public.attendance_docs for insert to authenticated with check (
  tutor_id = (select public.app_account_id())
);
create policy "attendance_docs admin" on public.attendance_docs for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- messages: one thread per tutor
create policy "messages read" on public.messages for select to authenticated using (
  tutor_id = (select public.app_account_id())
  or (select public.app_role()) = 'admin'
  or ((select public.app_role()) = 'lecturer' and public.lecturer_can_see(tutor_id))
);
create policy "messages lecturer send" on public.messages for insert to authenticated with check (
  (select public.app_role()) = 'lecturer'
  and sender_id = (select public.app_account_id())
  and sender_role = 'lecturer'
  and public.lecturer_can_see(tutor_id)
);
create policy "messages tutor read receipt" on public.messages for update to authenticated
  using (tutor_id = (select public.app_account_id()))
  with check (tutor_id = (select public.app_account_id()));
create policy "messages admin" on public.messages for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- programme settings: everyone reads (tutors need the rate), admin changes
create policy "settings read" on public.settings for select to authenticated using (true);
create policy "settings admin" on public.settings for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- response templates: admin only
create policy "response_templates admin" on public.response_templates for all to authenticated
  using ((select public.app_role()) = 'admin') with check ((select public.app_role()) = 'admin');

-- new tables
create policy "account_users read own" on public.account_users for select to authenticated using (
  user_id = (select auth.uid()) or (select public.app_role()) = 'admin'
);
create policy "user_settings own" on public.user_settings for all to authenticated
  using (account_id = (select public.app_account_id()))
  with check (account_id = (select public.app_account_id()));

-- Storage: registers live under <account_id>/<file>. Replace the open bucket policies.
drop policy if exists "registers read" on storage.objects;
drop policy if exists "registers write" on storage.objects;
drop policy if exists "registers update" on storage.objects;
drop policy if exists "registers delete" on storage.objects;

create policy "registers read" on storage.objects for select to authenticated using (
  bucket_id = 'attendance-registers'
  and ((storage.foldername(name))[1] = (select public.app_account_id()) or (select public.app_role()) = 'admin')
);
create policy "registers upload" on storage.objects for insert to authenticated with check (
  bucket_id = 'attendance-registers'
  and (storage.foldername(name))[1] = (select public.app_account_id())
);
create policy "registers admin delete" on storage.objects for delete to authenticated using (
  bucket_id = 'attendance-registers' and (select public.app_role()) = 'admin'
);

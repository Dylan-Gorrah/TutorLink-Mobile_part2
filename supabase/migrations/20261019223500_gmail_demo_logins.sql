-- Demo logins move to Gmail (Dylan, 19 Oct 22:35). Every demo account becomes a "+" alias of
-- Dylan's own Gmail (e.g. dylangorrah3+student1@gmail.com), so they are real addresses he owns.
-- Data changes only; no tables or columns change.

-- 1. Logins and profile emails
do $$
declare
  r record;
  new_email text;
begin
  for r in
    select u.id, u.email, au.account_id
    from auth.users u join public.account_users au on au.user_id = u.id
    where u.email like '%@tutorlink.demo'
  loop
    new_email := 'dylangorrah3+' || split_part(r.email, '@', 1) || '@gmail.com';
    update auth.users set email = new_email, updated_at = now() where id = r.id;
    update auth.identities
      set identity_data = jsonb_set(identity_data, '{email}', to_jsonb(new_email)), updated_at = now()
      where user_id = r.id and provider = 'email';
    update public.accounts set email = new_email where id = r.account_id;
  end loop;
end
$$;

-- 2. Remove the two lecturers registered while testing on the phone (no messages or sessions)
delete from public.account_modules where account_id in ('lect_48e6ad28c9', 'lect_bcf17b6512');
delete from public.user_settings where account_id in ('lect_48e6ad28c9', 'lect_bcf17b6512');
delete from auth.users where email in ('botha@rosebank.ac.za', 'lecture1@rosebank.ac.za');
delete from public.accounts where id in ('lect_48e6ad28c9', 'lect_bcf17b6512');

-- 3. Applicants: approving one now gives them a Gmail login
update public.applications
  set email = 'dylangorrah3+' || lower(split_part(name, ' ', 1)) || '@gmail.com'
  where email like '%@student.rosebank.ac.za';

-- 4. More activity so every screen has something to show
insert into public.messages (tutor_id, sender_id, sender_role, sender_name, topic, body, sent_at, read_by_tutor) values
  ('tutor_001', 'lect_001', 'lecturer', 'Dr M. Pretorius', 'Test 2 marks are out',
   'Test 2 averages came in at 61%. Please spend Thursday on recursion — most marks were lost on the base case.',
   now() - interval '25 minutes', false),
  ('tutor_002', 'admin_001', 'admin', 'Programme Admin', 'Register still missing',
   'Your register for 10 May hasn''t been uploaded yet. Upload it from Sessions so admin can confirm the session.',
   now() - interval '2 hours', false),
  ('tutor_011', 'lect_003', 'lecturer', 'Mr J. Botha', 'PROG6221 practical moved',
   'Wednesday''s practical moves to Lab 1 because of the network upgrade. Same time.',
   now() - interval '50 minutes', false),
  ('tutor_007', 'lect_003', 'lecturer', 'Mr J. Botha', 'Heuristic evaluation',
   'HCIN6222 starts heuristic evaluation this week. Bring Nielsen''s ten heuristics printed for the group.',
   now() - interval '1 day 3 hours', false);

insert into public.sessions (tutor_id, module_code, session_date, student_count, confirmed, paid, logged_by) values
  ('tutor_001', 'NETW1111', '2026-10-15', 9, false, false, 'tutor'),
  ('tutor_001', 'PROG1111', '2026-10-17', 12, false, false, 'tutor'),
  ('tutor_002', 'NETW1111', '2026-10-16', 8, false, false, 'tutor'),
  ('tutor_007', 'PROG1111', '2026-10-14', 6, false, false, 'tutor');

insert into public.schedule (tutor_id, module_code, day_label, time_label, venue, student_count, is_live, marked, sort_order) values
  ('tutor_007', 'HCIN6222', 'Today', '11:00 – 12:30', 'Room 201', 8, true, false, 1),
  ('tutor_007', 'PROG1111', 'Friday', '14:00 – 15:30', 'Lab 2', 10, false, false, 2),
  ('tutor_010', 'PROG6212', 'Today', '13:00 – 14:30', 'Lab 4', 9, true, false, 1);

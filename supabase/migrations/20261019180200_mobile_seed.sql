-- TutorLink Mobile v2 seed. Builds on the web demo's seed (4 accounts, 4 applications, ...)
-- so every table has 10+ realistic rows, and gives every account a Supabase Auth login.
-- Test logins (README): student1..student10, lecturer1..lecturer3, admin1 @tutorlink.demo,
-- plus dylangorrah3@gmail.com (Google SSO tutor). Password for all: 123456.

-- Passwords now live (hashed) in Supabase Auth, not in plain text here.
update public.accounts set password = 'managed by Supabase Auth'
where password <> 'managed by Supabase Auth';

-- accounts
insert into public.accounts (id, role, name, email, password, student_number, approved_at) values
  ('tutor_003', 'tutor', 'Lindiwe Dube',     'lindiwe.d@tutor.rosebank.ac.za', 'managed by Supabase Auth', 'ST10345678', '2026-02-20'),
  ('tutor_004', 'tutor', 'Kagiso Molefe',    'kagiso.m@tutor.rosebank.ac.za',  'managed by Supabase Auth', 'ST10356712', '2026-02-24'),
  ('tutor_005', 'tutor', 'Ayesha Patel',     'ayesha.p@tutor.rosebank.ac.za',  'managed by Supabase Auth', 'ST10367823', '2026-03-02'),
  ('tutor_006', 'tutor', 'Tumelo Khumalo',   'tumelo.k@tutor.rosebank.ac.za',  'managed by Supabase Auth', 'ST10378934', '2026-03-09'),
  ('tutor_007', 'tutor', 'Nadia van Wyk',    'nadia.vw@tutor.rosebank.ac.za',  'managed by Supabase Auth', 'ST10389045', '2026-03-18'),
  ('tutor_008', 'tutor', 'Lwazi Zulu',       'lwazi.z@tutor.rosebank.ac.za',   'managed by Supabase Auth', 'ST10390156', '2026-04-06'),
  ('tutor_009', 'tutor', 'Chloe Adams',      'chloe.a@tutor.rosebank.ac.za',   'managed by Supabase Auth', 'ST10301267', '2026-04-14'),
  ('tutor_010', 'tutor', 'Bongani Sithole',  'bongani.s@tutor.rosebank.ac.za', 'managed by Supabase Auth', 'ST10312378', '2026-05-04'),
  ('tutor_011', 'tutor', 'Dylan Gorrah',     'dylangorrah3@gmail.com',         'managed by Supabase Auth', 'ST10398445', '2026-05-12'),
  ('lect_002',  'lecturer', 'Ms T. Naidoo',  't.naidoo@rosebank.ac.za',        'managed by Supabase Auth', null, null),
  ('lect_003',  'lecturer', 'Mr J. Botha',   'j.botha@rosebank.ac.za',         'managed by Supabase Auth', null, null)
on conflict (id) do nothing;

-- account_modules (tutors tutor them, lecturers teach them)
insert into public.account_modules (account_id, module_code) values
  ('tutor_003', 'DATA6211'), ('tutor_003', 'DATA6222'),
  ('tutor_004', 'PROG6221'), ('tutor_004', 'PROG6212'),
  ('tutor_005', 'SAND6221'), ('tutor_005', 'IPMA6212'),
  ('tutor_006', 'NETW1111'), ('tutor_006', 'MATH1111'),
  ('tutor_007', 'HCIN6222'), ('tutor_007', 'PROG1111'),
  ('tutor_008', 'ISEC6321'), ('tutor_008', 'NETW1111'),
  ('tutor_009', 'MATH1111'), ('tutor_009', 'DATA6211'),
  ('tutor_010', 'PROG6212'), ('tutor_010', 'DATA6222'),
  ('tutor_011', 'ISEC6321'), ('tutor_011', 'PROG6221'), ('tutor_011', 'HCIN6222'),
  ('lect_002', 'DATA6211'), ('lect_002', 'DATA6222'), ('lect_002', 'SAND6221'),
  ('lect_003', 'PROG6221'), ('lect_003', 'PROG6212'), ('lect_003', 'ISEC6321'), ('lect_003', 'HCIN6222')
on conflict do nothing;

-- sessions (logged; pending until admin confirms)
insert into public.sessions (tutor_id, module_code, session_date, student_count, confirmed, paid, logged_by) values
  ('tutor_003', 'DATA6211', '2026-09-08', 12, true,  true,  'tutor'),
  ('tutor_003', 'DATA6222', '2026-09-15', 9,  true,  true,  'tutor'),
  ('tutor_003', 'DATA6211', '2026-10-06', 11, true,  false, 'admin'),
  ('tutor_004', 'PROG6221', '2026-09-22', 15, true,  false, 'tutor'),
  ('tutor_004', 'PROG6212', '2026-10-13', 8,  false, false, 'tutor'),
  ('tutor_005', 'SAND6221', '2026-09-29', 10, true,  false, 'tutor'),
  ('tutor_005', 'IPMA6212', '2026-10-14', 6,  false, false, 'tutor'),
  ('tutor_006', 'NETW1111', '2026-10-01', 13, true,  true,  'tutor'),
  ('tutor_006', 'MATH1111', '2026-10-15', 17, false, false, 'tutor'),
  ('tutor_007', 'HCIN6222', '2026-10-07', 7,  true,  false, 'tutor'),
  ('tutor_008', 'ISEC6321', '2026-10-08', 9,  true,  false, 'admin'),
  ('tutor_008', 'NETW1111', '2026-10-16', 11, false, false, 'tutor'),
  ('tutor_009', 'MATH1111', '2026-10-09', 14, true,  false, 'tutor'),
  ('tutor_010', 'PROG6212', '2026-10-12', 10, false, false, 'tutor'),
  ('tutor_011', 'ISEC6321', '2026-09-24', 12, true,  true,  'tutor'),
  ('tutor_011', 'PROG6221', '2026-10-01', 9,  true,  true,  'tutor'),
  ('tutor_011', 'HCIN6222', '2026-10-08', 8,  true,  false, 'tutor'),
  ('tutor_011', 'ISEC6321', '2026-10-15', 11, true,  false, 'admin'),
  ('tutor_011', 'PROG6221', '2026-10-16', 10, false, false, 'tutor');

-- schedule (upcoming, as the demo shows them)
insert into public.schedule (tutor_id, module_code, day_label, time_label, venue, student_count, is_live, marked, sort_order) values
  ('tutor_011', 'ISEC6321', 'Today',     '14:00 – 15:30', 'Lab 3',    12, true,  false, 1),
  ('tutor_011', 'PROG6221', 'Tomorrow',  '10:00 – 11:30', 'Lab 1',    10, false, false, 2),
  ('tutor_011', 'HCIN6222', 'Thursday',  '12:00 – 13:30', 'Room 201', 8,  false, false, 3),
  ('tutor_003', 'DATA6211', 'Today',     '09:00 – 10:30', 'Lab 5',    11, true,  false, 1),
  ('tutor_003', 'DATA6222', 'Wednesday', '13:00 – 14:30', 'Lab 5',    9,  false, false, 2),
  ('tutor_004', 'PROG6221', 'Tomorrow',  '08:00 – 09:30', 'Lab 2',    15, false, false, 1),
  ('tutor_005', 'SAND6221', 'Friday',    '11:00 – 12:30', 'Room 105', 10, false, false, 1),
  ('tutor_006', 'MATH1111', 'Today',     '15:30 – 17:00', 'Room 310', 17, true,  false, 1),
  ('tutor_008', 'ISEC6321', 'Thursday',  '09:00 – 10:30', 'Lab 4',    9,  false, false, 1),
  ('tutor_009', 'MATH1111', 'Wednesday', '10:00 – 11:30', 'Room 310', 14, false, false, 1);

-- attendance registers
insert into public.attendance_docs (tutor_id, module_code, file_name, uploaded_at) values
  ('tutor_003', 'DATA6211', 'Attendance_Sep_Week2.pdf',  '2026-09-09'),
  ('tutor_003', 'DATA6222', 'Attendance_Sep_Week3.pdf',  '2026-09-16'),
  ('tutor_004', 'PROG6221', 'Register_PROG6221_22Sep.pdf','2026-09-23'),
  ('tutor_005', 'SAND6221', 'Attendance_29_Sep.pdf',     '2026-09-30'),
  ('tutor_006', 'NETW1111', 'Attendance_Oct_Week1.pdf',  '2026-10-02'),
  ('tutor_008', 'ISEC6321', 'ISEC_Register_08Oct.jpg',   '2026-10-09'),
  ('tutor_011', 'ISEC6321', 'Attendance_Sep_Week4.pdf',  '2026-09-25'),
  ('tutor_011', 'HCIN6222', 'Attendance_Oct_Week2.pdf',  '2026-10-09');

-- messages (one thread per tutor)
insert into public.messages (tutor_id, sender_id, sender_role, sender_name, topic, body, sent_at, read_by_tutor) values
  ('tutor_011', 'admin_001', 'admin', 'Programme Admin', 'Welcome to the tutoring programme',
   'Congratulations! Your application to join the Rosebank Tutoring Programme has been approved. You will receive onboarding information within 2–3 business days. Welcome to the team.',
   '2026-05-12 09:15+02', true),
  ('tutor_011', 'lect_003', 'lecturer', 'Mr J. Botha', 'Threat modelling recap',
   'The ISEC6321 class struggled with STRIDE on the last quiz. Spend the first 20 minutes walking through the banking app example before the practical.',
   now() - interval '3 hours', false),
  ('tutor_011', 'admin_001', 'admin', 'Programme Admin', 'Register for 16 October',
   'We have your session for 16 October logged but no register yet. Upload the signed register so it can be confirmed in this payment run.',
   now() - interval '1 day', false),
  ('tutor_011', 'lect_003', 'lecturer', 'Mr J. Botha', 'Usability testing week',
   'HCIN6222 students run their usability tests next week. Help them script tasks and keep each test under 15 minutes.',
   now() - interval '4 days', true),
  ('tutor_003', 'lect_002', 'lecturer', 'Ms T. Naidoo', 'Normalisation before joins',
   'Please revisit 3NF with the DATA6211 group before we start joins. Use the student-course example from the workbook.',
   now() - interval '6 hours', false),
  ('tutor_003', 'admin_001', 'admin', 'Programme Admin', 'September paid',
   'Your September sessions have been paid at R150 per confirmed session. Check your statement in the app.',
   now() - interval '9 days', true),
  ('tutor_004', 'lect_003', 'lecturer', 'Mr J. Botha', 'Exception handling',
   'PROG6221 practical 6 is on try-catch-finally. Make sure everyone runs the file-reading example before they leave.',
   now() - interval '2 days', false),
  ('tutor_005', 'lect_002', 'lecturer', 'Ms T. Naidoo', 'Use case diagrams',
   'SAND6221 assignment 2 needs use case diagrams with includes and extends. Bring two worked examples.',
   now() - interval '5 days', true),
  ('tutor_006', 'admin_001', 'admin', 'Programme Admin', 'Venue change Friday',
   'Room 310 is booked for exams on Friday. Your MATH1111 session moves to Room 312, same time.',
   now() - interval '20 hours', false),
  ('tutor_007', 'lect_001', 'lecturer', 'Dr M. Pretorius', 'Loops revision',
   'PROG1111 test 2 covers while and do-while loops. Please run a short revision quiz at the start of your next session.',
   now() - interval '2 days', true),
  ('tutor_008', 'lect_001', 'lecturer', 'Dr M. Pretorius', 'Subnet masks',
   'The NETW1111 group still mixes up CIDR notation. Do five quick conversions on the board before the lab.',
   now() - interval '7 hours', false),
  ('tutor_009', 'admin_001', 'admin', 'Programme Admin', 'Contract renewal',
   'Your tutor contract runs to 30 November. Sign the renewal in the admin office before month end.',
   now() - interval '3 days', true),
  ('tutor_010', 'lect_002', 'lecturer', 'Ms T. Naidoo', 'Stored procedures',
   'DATA6222 moves to stored procedures this week. Have the sample database restored on the lab machines first.',
   now() - interval '1 day', false);

-- applications (from the website)
insert into public.applications (id, name, email, student_number, programme, status, background_checked, response_message, submitted_at, processed_at) values
  ('app_005', 'Naledi Mahlangu', 'naledi.m@student.rosebank.ac.za', 'ST10411234', 'Diploma in IT — Software Development', 'pending',      false, '', '2026-10-05', null),
  ('app_006', 'Ruan Steyn',      'ruan.s@student.rosebank.ac.za',   'ST10422345', 'Diploma in IT — Software Development', 'under_review', true,  '', '2026-10-02', null),
  ('app_007', 'Zanele Mthembu',  'zanele.m@student.rosebank.ac.za', 'ST10433456', 'Diploma in IT — Network Engineering',  'pending',      false, '', '2026-10-08', null),
  ('app_008', 'Ethan Pillay',    'ethan.p@student.rosebank.ac.za',  'ST10444567', 'Diploma in IT — Software Development', 'rejected',     true,
   'After reviewing your academic record, your application does not meet the required 75% threshold for the module(s) you selected. We encourage you to reapply once you have achieved the required academic standing.',
   '2026-09-20', '2026-09-26'),
  ('app_009', 'Precious Ngcobo', 'precious.n@student.rosebank.ac.za','ST10455678','Diploma in IT — Software Development', 'pending',      false, '', '2026-10-11', null),
  ('app_010', 'Mpho Radebe',     'mpho.r@student.rosebank.ac.za',   'ST10466789', 'Diploma in IT — Software Development', 'rejected',     true,
   'Thank you for your application. The tutor position for the selected module(s) has already been filled for this intake. We encourage you to apply again in the next cycle.',
   '2026-09-15', '2026-09-22'),
  ('app_011', 'Keanu Davids',    'keanu.d@student.rosebank.ac.za',  'ST10477890', 'Diploma in IT — Software Development', 'under_review', true,  '', '2026-10-09', null)
on conflict (id) do nothing;

insert into public.application_modules (application_id, module_code) values
  ('app_005', 'PROG6221'),
  ('app_006', 'DATA6211'),
  ('app_007', 'NETW1111'), ('app_007', 'ISEC6321'),
  ('app_008', 'PROG1111'),
  ('app_009', 'HCIN6222'), ('app_009', 'SAND6221'),
  ('app_010', 'IPMA6212'),
  ('app_011', 'MATH1111')
on conflict do nothing;

insert into public.report_modules (application_id, module_code, mark, result) values
  ('app_005', 'PROG6221', 84, 'PD'), ('app_005', 'PROG1111', 79, 'PD'),
  ('app_006', 'DATA6211', 88, 'PD'), ('app_006', 'MATH1111', 74, 'P'),
  ('app_007', 'NETW1111', 81, 'PD'), ('app_007', 'ISEC6321', 69, 'P'),
  ('app_008', 'PROG1111', 61, 'P'),
  ('app_009', 'HCIN6222', 90, 'PD'), ('app_009', 'SAND6221', 83, 'PD'),
  ('app_010', 'IPMA6212', 80, 'PD'),
  ('app_011', 'MATH1111', 93, 'PD'), ('app_011', 'PROG1111', 85, 'PD');

-- programme settings (key/value)
insert into public.settings (key, value) values
  ('programme_name', 'Rosebank Tutoring Programme'),
  ('currency', 'ZAR'),
  ('payment_day', '25'),
  ('max_students_per_session', '60'),
  ('session_length_minutes', '90'),
  ('campus_domain', 'rosebank.ac.za'),
  ('register_deadline_days', '7'),
  ('semester_end', '2026-11-30'),
  ('support_email', 'tutoring@rosebank.ac.za')
on conflict (key) do nothing;

-- response templates
insert into public.response_templates (id, label, body, sort_order) values
  (5,  'Approved — Selected Modules Only', 'Congratulations! You have been approved to tutor some of the modules you applied for. Your profile shows the modules you may tutor this semester. Welcome to the team.', 5),
  (6,  'Background Check Outstanding', 'Your application looks strong, but we could not complete your background check. Please visit the admin office with your ID so we can finish processing your application.', 6),
  (7,  'Late Application', 'Thank you for applying. Applications for this intake closed before yours arrived. Please apply again when the next cycle opens.', 7),
  (8,  'Approved — Starts Next Term', 'Congratulations! Your application has been approved. Tutoring for your modules starts next term, and we will send your schedule before then.', 8),
  (9,  'Duplicate Application', 'We already have an application from you on file for this intake. We will process your original application and close this one.', 9),
  (10, 'Timetable Clash', 'Thank you for applying. The tutoring slots for your selected module(s) clash with your own class timetable, so we cannot place you this semester.', 10)
on conflict (id) do nothing;

-- Supabase Auth logins (password 123456, hashed with bcrypt) linked to accounts.
do $$
declare
  r   record;
  uid uuid;
begin
  for r in
    select * from (values
      ('student1@tutorlink.demo',  'tutor_001'),
      ('student2@tutorlink.demo',  'tutor_002'),
      ('student3@tutorlink.demo',  'tutor_003'),
      ('student4@tutorlink.demo',  'tutor_004'),
      ('student5@tutorlink.demo',  'tutor_005'),
      ('student6@tutorlink.demo',  'tutor_006'),
      ('student7@tutorlink.demo',  'tutor_007'),
      ('student8@tutorlink.demo',  'tutor_008'),
      ('student9@tutorlink.demo',  'tutor_009'),
      ('student10@tutorlink.demo', 'tutor_010'),
      ('dylangorrah3@gmail.com',   'tutor_011'),
      ('lecturer1@tutorlink.demo', 'lect_001'),
      ('lecturer2@tutorlink.demo', 'lect_002'),
      ('lecturer3@tutorlink.demo', 'lect_003'),
      ('admin1@tutorlink.demo',    'admin_001')
    ) v(email, account_id)
  loop
    if exists (select 1 from auth.users where email = r.email) then
      continue;
    end if;
    uid := gen_random_uuid();
    insert into auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
      confirmation_token, recovery_token, email_change_token_new, email_change,
      email_change_token_current, phone_change, phone_change_token, reauthentication_token
    ) values (
      '00000000-0000-0000-0000-000000000000', uid, 'authenticated', 'authenticated', r.email,
      extensions.crypt('123456', extensions.gen_salt('bf')), now(),
      '{"provider":"email","providers":["email"]}', '{"email_verified":true}', now(), now(),
      '', '', '', '', '', '', '', ''
    );
    insert into auth.identities (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
    values (uid::text, uid,
            jsonb_build_object('sub', uid::text, 'email', r.email, 'email_verified', true, 'phone_verified', false),
            'email', now(), now(), now());
    insert into public.account_users (user_id, account_id) values (uid, r.account_id);
  end loop;
end
$$;

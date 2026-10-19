-- Give each website applicant (except the one already approved) an app login, so they can
-- sign in as a student and see their application status. Password 123456 (testing only).
do $$
declare
  r   record;
  uid uuid;
begin
  for r in
    select a.id, a.email from public.applications a
    where a.status <> 'approved'
      and not exists (select 1 from public.application_users x where x.application_id = a.id)
      and not exists (select 1 from auth.users u where u.email = a.email)
  loop
    uid := gen_random_uuid();
    insert into auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
      confirmation_token, recovery_token, email_change_token_new, email_change,
      email_change_token_current, phone_change, phone_change_token, reauthentication_token
    ) values (
      '00000000-0000-0000-0000-000000000000', uid, 'authenticated', 'authenticated', r.email,
      extensions.crypt('123456', extensions.gen_salt('bf')), now(),
      '{"provider":"email","providers":["email"]}', '{"email_verified":true,"password_set":true}', now(), now(),
      '', '', '', '', '', '', '', ''
    );
    insert into auth.identities (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
    values (uid::text, uid, jsonb_build_object('sub', uid::text, 'email', r.email, 'email_verified', true, 'phone_verified', false),
            'email', now(), now(), now());
    insert into public.application_users (user_id, application_id) values (uid, r.id);
  end loop;
end
$$;

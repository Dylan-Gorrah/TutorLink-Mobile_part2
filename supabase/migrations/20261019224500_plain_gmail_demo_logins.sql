-- Dylan, 19 Oct 22:45: demo logins become short Gmail addresses (student1@gmail.com, ...),
-- password 123456, for testing only. Data changes only.

do $$
declare
  r record;
  new_email text;
begin
  for r in
    select u.id, u.email, au.account_id
    from auth.users u join public.account_users au on au.user_id = u.id
    where u.email like 'dylangorrah3+%@gmail.com'
  loop
    new_email := substring(split_part(r.email, '@', 1) from 'dylangorrah3\+(.*)') || '@gmail.com';
    update auth.users set email = new_email, updated_at = now() where id = r.id;
    update auth.identities
      set identity_data = jsonb_set(identity_data, '{email}', to_jsonb(new_email)), updated_at = now()
      where user_id = r.id and provider = 'email';
    update public.accounts set email = new_email where id = r.account_id;
  end loop;
end
$$;

update public.applications
  set email = substring(split_part(email, '@', 1) from 'dylangorrah3\+(.*)') || '@gmail.com'
  where email like 'dylangorrah3+%@gmail.com';

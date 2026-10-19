-- TutorLink Mobile v2: link Supabase Auth logins to the existing accounts table,
-- add personal settings, and the helper functions the RLS policies use.
-- Add-only: no existing table or column is renamed, dropped or altered.

-- One Supabase Auth login per TutorLink account.
create table public.account_users (
  user_id    uuid primary key references auth.users (id) on delete cascade,
  account_id text not null unique references public.accounts (id) on delete cascade,
  created_at timestamptz not null default now()
);

-- Personal settings every user can change from their Profile.
create table public.user_settings (
  account_id       text primary key references public.accounts (id) on delete cascade,
  haptics          boolean not null default true,
  reduce_motion    boolean not null default false,
  message_previews boolean not null default true,
  updated_at       timestamptz not null default now()
);

alter table public.account_users enable row level security;
alter table public.user_settings enable row level security;

-- Every new account gets a settings row straight away.
create or replace function public.create_user_settings()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.user_settings (account_id) values (new.id)
  on conflict (account_id) do nothing;
  return new;
end;
$$;

create trigger accounts_create_user_settings
  after insert on public.accounts
  for each row execute function public.create_user_settings();

insert into public.user_settings (account_id)
select id from public.accounts
on conflict (account_id) do nothing;

-- Helpers for RLS. Security definer so the policies don't recurse into each other.
create or replace function public.app_account_id()
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select account_id from public.account_users where user_id = auth.uid()
$$;

create or replace function public.app_role()
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select a.role from public.accounts a
  join public.account_users u on u.account_id = a.id
  where u.user_id = auth.uid()
$$;

-- True when the signed-in lecturer teaches a module the tutor tutors.
create or replace function public.lecturer_can_see(tutor text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.account_modules lm
    join public.account_modules tm on tm.module_code = lm.module_code
    join public.accounts t on t.id = tm.account_id and t.role = 'tutor'
    where lm.account_id = public.app_account_id()
      and tm.account_id = tutor
  )
$$;

revoke execute on function public.create_user_settings() from public, anon, authenticated;
revoke execute on function public.app_account_id() from public, anon;
revoke execute on function public.app_role() from public, anon;
revoke execute on function public.lecturer_can_see(text) from public, anon;
grant execute on function public.app_account_id() to authenticated;
grant execute on function public.app_role() to authenticated;
grant execute on function public.lecturer_can_see(text) to authenticated;

-- Students register in the app and apply to tutor. This links a student's Supabase login
-- to their application, so approving it upgrades that same login to a tutor account.
-- Add-only: no existing table or column changes.

create table public.application_users (
  user_id        uuid primary key references auth.users (id) on delete cascade,
  application_id text not null unique references public.applications (id) on delete cascade,
  created_at     timestamptz not null default now()
);

alter table public.application_users enable row level security;

create policy "application_users read own" on public.application_users for select to authenticated using (
  user_id = (select auth.uid()) or (select public.app_role()) = 'admin'
);

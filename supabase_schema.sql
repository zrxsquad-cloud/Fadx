-- ====================================================================
-- FADX SOCIAL APP - COMPLETE SUPABASE DATABASE SCHEMA & RLS POLICIES
-- ====================================================================
-- Instructions:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/
-- 2. Go to the "SQL Editor" section on the left sidebar.
-- 3. Click "New Query", paste all the code below, and click "Run" (Execute).
-- ====================================================================

-- 1. ENABLE UUID EXTENSION
create extension if not exists "uuid-ossp";

-- ====================================================================
-- 2. PROFILES TABLE (Public user profiles linked to auth.users)
-- ====================================================================
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text unique,
    full_name text,
    name text,
    avatar_url text,
    cover_url text,
    bio text default '',
    website text default '',
    phone text default '',
    location text default '',
    followers_count integer default 0,
    following_count integer default 0,
    friends_count integer default 0,
    is_verified boolean default false,
    is_online boolean default false,
    last_active timestamp with time zone default now(),
    gender text,
    dob text,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

-- Enable RLS
alter table public.profiles enable row level security;

-- Policies for profiles
create policy "Allow public read of profiles"
    on public.profiles for select
    using (true);

create policy "Allow users to insert their own profile"
    on public.profiles for insert
    with check (auth.uid() = id);

create policy "Allow users to update their own profile"
    on public.profiles for update
    using (auth.uid() = id)
    with check (auth.uid() = id);

create policy "Allow users to delete their own profile"
    on public.profiles for delete
    using (auth.uid() = id);

-- ====================================================================
-- 3. POSTS TABLE
-- ====================================================================
create table if not exists public.posts (
    id text primary key default ('post_' || uuid_generate_v4()::text),
    user_id uuid not null references auth.users(id) on delete cascade,
    author_name text default '',
    author_username text default '',
    author_avatar text default '',
    content text not null default '',
    media_urls jsonb default '[]'::jsonb,
    media_type text default 'NONE',
    visibility text default 'PUBLIC',
    location text,
    likes_count integer default 0,
    comments_count integer default 0,
    shares_count integer default 0,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

-- Enable RLS
alter table public.posts enable row level security;

-- Policies for posts
create policy "Allow public to read public posts"
    on public.posts for select
    using (visibility = 'PUBLIC' or auth.uid() = user_id);

create policy "Allow authenticated users to create posts"
    on public.posts for insert
    with check (auth.uid() = user_id);

create policy "Allow authors to update their posts"
    on public.posts for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "Allow authors to delete their posts"
    on public.posts for delete
    using (auth.uid() = user_id);

-- ====================================================================
-- 4. POST COMMENTS TABLE
-- ====================================================================
create table if not exists public.comments (
    id text primary key default ('comment_' || uuid_generate_v4()::text),
    post_id text not null references public.posts(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    user_name text default '',
    user_avatar text default '',
    content text not null,
    created_at timestamp with time zone default now()
);

-- Enable RLS
alter table public.comments enable row level security;

-- Policies for comments
create policy "Allow read comments"
    on public.comments for select
    using (true);

create policy "Allow authenticated users to create comments"
    on public.comments for insert
    with check (auth.uid() = user_id);

create policy "Allow authors to delete comments"
    on public.comments for delete
    using (auth.uid() = user_id);

-- ====================================================================
-- 5. POST LIKES & REACTIONS TABLE
-- ====================================================================
create table if not exists public.post_likes (
    post_id text not null references public.posts(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    reaction_type text default 'LIKE',
    created_at timestamp with time zone default now(),
    primary key (post_id, user_id)
);

-- Enable RLS
alter table public.post_likes enable row level security;

create policy "Allow read likes"
    on public.post_likes for select
    using (true);

create policy "Allow authenticated users to add likes"
    on public.post_likes for insert
    with check (auth.uid() = user_id);

create policy "Allow users to remove their likes"
    on public.post_likes for delete
    using (auth.uid() = user_id);

-- ====================================================================
-- 6. BLOCKED USERS (User safety & Google Play UGC compliance)
-- ====================================================================
create table if not exists public.blocked_users (
    blocker_id uuid not null references auth.users(id) on delete cascade,
    blocked_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamp with time zone default now(),
    primary key (blocker_id, blocked_id)
);

-- Enable RLS
alter table public.blocked_users enable row level security;

create policy "Users can view their own block list"
    on public.blocked_users for select
    using (auth.uid() = blocker_id);

create policy "Users can block other users"
    on public.blocked_users for insert
    with check (auth.uid() = blocker_id);

create policy "Users can unblock users"
    on public.blocked_users for delete
    using (auth.uid() = blocker_id);

-- ====================================================================
-- 7. CONTENT REPORTS (Google Play UGC & Moderation compliance)
-- ====================================================================
create table if not exists public.reports (
    id text primary key default ('rep_' || uuid_generate_v4()::text),
    reporter_id uuid not null references auth.users(id) on delete cascade,
    target_type text not null, -- 'post', 'profile', 'comment'
    target_id text not null,
    target_summary text default '',
    reason text not null,
    status text default 'Pending',
    created_at timestamp with time zone default now()
);

-- Enable RLS
alter table public.reports enable row level security;

create policy "Users can submit reports"
    on public.reports for insert
    with check (auth.uid() = reporter_id);

create policy "Users can view their own reports"
    on public.reports for select
    using (auth.uid() = reporter_id);

-- ====================================================================
-- 8. AUTOMATIC PROFILE CREATION TRIGGER ON SIGNUP
-- ====================================================================
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, username, full_name, name, avatar_url)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data->>'name', coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1))),
    coalesce(new.raw_user_meta_data->>'name', coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1))),
    coalesce(new.raw_user_meta_data->>'avatar_url', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500&q=80')
  )
  on conflict (id) do update set
    updated_at = now();
  return new;
end;
$$ language plpgsql security definer;

-- Trigger execution
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ====================================================================
-- 9. STORAGE BUCKETS CONFIGURATION (Avatars & Posts)
-- ====================================================================
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('posts', 'posts', true)
on conflict (id) do nothing;

-- Storage policies for avatars
create policy "Avatars are publicly accessible"
    on storage.objects for select
    using (bucket_id = 'avatars');

create policy "Anyone authenticated can upload an avatar"
    on storage.objects for insert
    with check (bucket_id = 'avatars' and auth.role() = 'authenticated');

create policy "Users can update their avatar"
    on storage.objects for update
    using (bucket_id = 'avatars' and auth.role() = 'authenticated');

-- Storage policies for posts
create policy "Post media is publicly accessible"
    on storage.objects for select
    using (bucket_id = 'posts');

create policy "Anyone authenticated can upload post media"
    on storage.objects for insert
    with check (bucket_id = 'posts' and auth.role() = 'authenticated');

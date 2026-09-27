-- Cite Circle: Cloud Sync for Repository Collections, Papers, Reading Status & Notes
-- Migration: 20260925_user_library_sync.sql

-- 1. Create user_library_papers table
CREATE TABLE IF NOT EXISTS public.user_library_papers (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    authors TEXT NOT NULL DEFAULT '',
    year TEXT NOT NULL DEFAULT '',
    venue TEXT NOT NULL DEFAULT '',
    doi TEXT NOT NULL DEFAULT '',
    url TEXT NOT NULL DEFAULT '',
    pdf_url TEXT NOT NULL DEFAULT '',
    abstract_text TEXT NOT NULL DEFAULT '',
    open_access BOOLEAN NOT NULL DEFAULT false,
    content TEXT NOT NULL DEFAULT '',
    author_name TEXT NOT NULL DEFAULT '',
    author_initials TEXT NOT NULL DEFAULT '',
    affiliation TEXT NOT NULL DEFAULT '',
    citation_override TEXT NOT NULL DEFAULT '',
    reading_status TEXT NOT NULL DEFAULT 'TO_READ',
    research_notes TEXT NOT NULL DEFAULT '',
    is_bookmarked BOOLEAN NOT NULL DEFAULT false,
    published_at BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, id)
);

-- 2. Create user_collections table
CREATE TABLE IF NOT EXISTS public.user_collections (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    color_hex TEXT NOT NULL DEFAULT '#1A73E8',
    icon_name TEXT NOT NULL DEFAULT 'folder',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, id)
);

-- 3. Create user_collection_papers table
CREATE TABLE IF NOT EXISTS public.user_collection_papers (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    collection_id TEXT NOT NULL,
    paper_id TEXT NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, collection_id, paper_id),
    CONSTRAINT fk_user_collection FOREIGN KEY (user_id, collection_id) 
        REFERENCES public.user_collections(user_id, id) ON DELETE CASCADE
);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.user_library_papers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_collection_papers ENABLE ROW LEVEL SECURITY;

-- 5. Role Grants
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_library_papers TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_collections TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_collection_papers TO authenticated;

-- 6. Policies for user_library_papers
DROP POLICY IF EXISTS "Users can view their own library papers" ON public.user_library_papers;
CREATE POLICY "Users can view their own library papers" ON public.user_library_papers
    FOR SELECT TO authenticated
    USING ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert their own library papers" ON public.user_library_papers;
CREATE POLICY "Users can insert their own library papers" ON public.user_library_papers
    FOR INSERT TO authenticated
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update their own library papers" ON public.user_library_papers;
CREATE POLICY "Users can update their own library papers" ON public.user_library_papers
    FOR UPDATE TO authenticated
    USING ((SELECT auth.uid()) = user_id)
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete their own library papers" ON public.user_library_papers;
CREATE POLICY "Users can delete their own library papers" ON public.user_library_papers
    FOR DELETE TO authenticated
    USING ((SELECT auth.uid()) = user_id);

-- 7. Policies for user_collections
DROP POLICY IF EXISTS "Users can view their own collections" ON public.user_collections;
CREATE POLICY "Users can view their own collections" ON public.user_collections
    FOR SELECT TO authenticated
    USING ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert their own collections" ON public.user_collections;
CREATE POLICY "Users can insert their own collections" ON public.user_collections
    FOR INSERT TO authenticated
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update their own collections" ON public.user_collections;
CREATE POLICY "Users can update their own collections" ON public.user_collections
    FOR UPDATE TO authenticated
    USING ((SELECT auth.uid()) = user_id)
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete their own collections" ON public.user_collections;
CREATE POLICY "Users can delete their own collections" ON public.user_collections
    FOR DELETE TO authenticated
    USING ((SELECT auth.uid()) = user_id);

-- 8. Policies for user_collection_papers
DROP POLICY IF EXISTS "Users can view their own collection papers" ON public.user_collection_papers;
CREATE POLICY "Users can view their own collection papers" ON public.user_collection_papers
    FOR SELECT TO authenticated
    USING ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can insert their own collection papers" ON public.user_collection_papers;
CREATE POLICY "Users can insert their own collection papers" ON public.user_collection_papers
    FOR INSERT TO authenticated
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update their own collection papers" ON public.user_collection_papers;
CREATE POLICY "Users can update their own collection papers" ON public.user_collection_papers
    FOR UPDATE TO authenticated
    USING ((SELECT auth.uid()) = user_id)
    WITH CHECK ((SELECT auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete their own collection papers" ON public.user_collection_papers;
CREATE POLICY "Users can delete their own collection papers" ON public.user_collection_papers
    FOR DELETE TO authenticated
    USING ((SELECT auth.uid()) = user_id);

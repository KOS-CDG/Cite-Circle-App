-- Add reading progress & page bookmarks to user_library_papers
ALTER TABLE public.user_library_papers 
    ADD COLUMN IF NOT EXISTS last_read_page INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS total_page_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS page_bookmarks TEXT NOT NULL DEFAULT '';

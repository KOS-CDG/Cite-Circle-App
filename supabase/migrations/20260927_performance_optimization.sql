-- Cite Circle Performance & Facebook-Scale Optimization Migration
-- File: 20260927_performance_optimization.sql

-- 1. Strategic Composite & Covering Indexes for Facebook-Style Fast Browsing
CREATE INDEX IF NOT EXISTS idx_posts_created_at_desc 
    ON public.posts (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_posts_user_created 
    ON public.posts (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_post_likes_user_post 
    ON public.post_likes (user_id, post_id);

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id 
    ON public.post_likes (post_id);

CREATE INDEX IF NOT EXISTS idx_comments_post_created 
    ON public.comments (post_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread 
    ON public.notifications (recipient_id, created_at DESC) 
    WHERE is_read = false;

CREATE INDEX IF NOT EXISTS idx_messages_conversation_created 
    ON public.messages (conversation_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_saved_posts_user_post 
    ON public.saved_posts (user_id, post_id);

-- 2. Optimize Message Notification Fanout (Atomic Set-Based INSERT ... SELECT)
-- Eliminates O(N) procedural PL/pgSQL loop which locks database under high concurrency
CREATE OR REPLACE FUNCTION public.notify_on_message()
RETURNS TRIGGER 
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.notifications (recipient_id, actor_id, type, post_id, is_read, created_at)
  SELECT user_id, NEW.sender_id, 'message'::public.notification_type, NULL, false, NOW()
  FROM public.conversation_participants
  WHERE conversation_id = NEW.conversation_id AND user_id != NEW.sender_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Row-Lock Serialization & Counter Safety
CREATE OR REPLACE FUNCTION public.update_post_likes_count()
RETURNS TRIGGER 
SET search_path = public
AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.posts SET likes_count = GREATEST(0, likes_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.update_post_comments_count()
RETURNS TRIGGER 
SET search_path = public
AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.posts SET comments_count = GREATEST(0, comments_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Security Hardening on Internal Triggers (Revoke RPC direct invocations & fix search path)
ALTER FUNCTION public.notify_on_comment() SET search_path = public;
ALTER FUNCTION public.notify_on_post_like() SET search_path = public;

REVOKE EXECUTE ON FUNCTION public.notify_on_comment() FROM public, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.notify_on_message() FROM public, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.notify_on_post_like() FROM public, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.update_post_likes_count() FROM public, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.update_post_comments_count() FROM public, anon, authenticated;

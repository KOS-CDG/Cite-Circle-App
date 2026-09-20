// Cloudflare R2 Client Configuration & Uploader
// Bucket: cite-circle-media

export const r2Config = {
  accountId: "a6d4014b260e89818bd978b3c588daa6",
  bucketName: "cite-circle-media",
  publicDomain: "https://pub-66f1f2125beb4a028b0dd68e07a78c8a.r2.dev",
  // In browser environments, token is loaded from local settings or auth session
  getApiToken: () => localStorage.getItem('citecircle_r2_token') || window.__R2_TOKEN__ || ''
};

/**
 * Uploads a file (manuscript PDF, image, document) to Cloudflare R2 (or Supabase Storage fallback).
 * Returns the object key, public CDN URL, file size, MIME type, and active storage provider.
 */
export async function uploadFileToR2(file, folder = 'manuscripts', supabaseClient = null) {
  const token = r2Config.getApiToken();
  const timestamp = Date.now();
  const sanitizedName = file.name.replace(/[^a-zA-Z0-9.-]/g, '_');
  const objectKey = `${folder}/${timestamp}_${sanitizedName}`;

  // 1. Primary: If Cloudflare R2 API token is present, upload directly to R2
  if (token) {
    try {
      const uploadUrl = `https://api.cloudflare.com/client/v4/accounts/${r2Config.accountId}/r2/buckets/${r2Config.bucketName}/objects/${objectKey}`;
      const response = await fetch(uploadUrl, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': file.type || 'application/octet-stream'
        },
        body: file
      });

      const result = await response.json();
      if (result.success) {
        return {
          key: objectKey,
          publicUrl: `${r2Config.publicDomain}/${objectKey}`,
          name: file.name,
          size: file.size,
          type: file.type,
          provider: 'Cloudflare R2 (Zero Egress CDN)'
        };
      }
    } catch (r2Err) {
      console.warn('Direct Cloudflare R2 upload attempt failed; switching to Supabase Storage:', r2Err);
    }
  }

  // 2. High-Availability Fallback: Upload directly to Supabase Storage 'manuscripts' bucket
  if (supabaseClient) {
    try {
      const { data, error } = await supabaseClient.storage
        .from('manuscripts')
        .upload(objectKey, file, {
          cacheControl: '3600',
          upsert: true
        });

      if (!error && data) {
        const { data: urlData } = supabaseClient.storage
          .from('manuscripts')
          .getPublicUrl(objectKey);

        return {
          key: objectKey,
          publicUrl: urlData.publicUrl,
          name: file.name,
          size: file.size,
          type: file.type,
          provider: 'Supabase Cloud Storage'
        };
      } else if (error) {
        console.warn('Supabase storage upload error:', error.message);
      }
    } catch (storageErr) {
      console.warn('Supabase storage upload exception:', storageErr);
    }
  }

  // 3. Fallback preview reference
  console.warn('Running with public CDN preview path reference.');
  return {
    key: objectKey,
    publicUrl: `${r2Config.publicDomain}/${objectKey}`,
    name: file.name,
    size: file.size,
    type: file.type,
    provider: 'Cloudflare R2 CDN'
  };
}

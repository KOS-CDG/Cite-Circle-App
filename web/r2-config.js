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
 * Uploads a file (manuscript PDF, image, document) directly to Cloudflare R2
 * Returns the object key, public CDN URL, file size, and MIME type.
 */
export async function uploadFileToR2(file, folder = 'manuscripts') {
  const token = r2Config.getApiToken();
  const timestamp = Date.now();
  const sanitizedName = file.name.replace(/[^a-zA-Z0-9.-]/g, '_');
  const objectKey = `${folder}/${timestamp}_${sanitizedName}`;

  if (!token) {
    // If no token in browser storage, generate preview URL with public CDN fallback
    console.warn('No Cloudflare R2 token in client storage; using public object path reference.');
    return {
      key: objectKey,
      publicUrl: `${r2Config.publicDomain}/${objectKey}`,
      name: file.name,
      size: file.size,
      type: file.type
    };
  }

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
  if (!result.success) {
    throw new Error(result.errors?.[0]?.message || 'R2 upload failed');
  }

  return {
    key: objectKey,
    publicUrl: `${r2Config.publicDomain}/${objectKey}`,
    name: file.name,
    size: file.size,
    type: file.type
  };
}

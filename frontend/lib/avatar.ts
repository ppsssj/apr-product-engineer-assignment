const AVATAR_PATHS = [
  "/avatar-1.png",
  "/avatar-2.png",
  "/avatar-3.png",
] as const;

// 32-bit FNV-1a over UTF-16 code units: deterministic across renders,
// sessions, and server/client, so one author always maps to one avatar.
function stableHash(value: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

// Avatar policy (PRODUCT_DECISION): stableHash(author) % 3.
export function avatarFor(author: string): string {
  return AVATAR_PATHS[stableHash(author) % AVATAR_PATHS.length];
}

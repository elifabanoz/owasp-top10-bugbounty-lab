export type Profile = 'secure' | 'vulnerable';
export type User = { id: number; username: string; role: string };
export type Report = { id: number; authorId: number; title: string; description: string; severity: string; status: string };
export type Comment = { id: number; authorId: number; body: string };
export type Program = { id: number; name: string; description: string };

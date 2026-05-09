"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";
import { useEffect } from "react";

const nav = [
  { href: "/app", label: "Visão geral" },
  { href: "/app/databases", label: "Bancos" },
  { href: "/app/settings", label: "BYOK / Config" },
  { href: "/app/suggestions", label: "Sugestões" },
  { href: "/app/snapshots", label: "Snapshots" },
  { href: "/app/audit", label: "Auditoria" },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) router.replace("/login");
  }, [isAuthenticated, router]);

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-50">
      <div className="mx-auto max-w-6xl px-6 py-8">
        <div className="flex items-center justify-between">
          <div className="font-semibold tracking-tight">DBA Agent</div>
          <button
            className="text-sm text-zinc-300 hover:text-white"
            onClick={() => {
              logout();
              router.replace("/login");
            }}
          >
            Sair
          </button>
        </div>

        <div className="mt-6 grid grid-cols-1 gap-6 md:grid-cols-[220px_1fr]">
          <aside className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
            <nav className="space-y-1">
              {nav.map((item) => {
                const active = pathname === item.href;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={[
                      "block rounded-lg px-3 py-2 text-sm",
                      active
                        ? "bg-zinc-950 border border-zinc-800 text-white"
                        : "text-zinc-300 hover:bg-zinc-950/60 hover:text-white",
                    ].join(" ")}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </aside>

          <main className="rounded-2xl border border-zinc-800 bg-zinc-900 p-6">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}


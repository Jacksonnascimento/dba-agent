"use client";

import Link from "next/link";

export default function DashboardPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Visão geral</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Configure bancos, BYOK e aprove sugestões para o agente executar.
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Link
          className="rounded-xl border border-zinc-800 bg-zinc-950/40 p-4 hover:bg-zinc-950/60"
          href="/app/databases"
        >
          <div className="text-sm font-medium">Bancos</div>
          <div className="mt-1 text-xs text-zinc-400">
            Cadastre múltiplos bancos por tenant e gere tokens do agente.
          </div>
        </Link>
        <Link
          className="rounded-xl border border-zinc-800 bg-zinc-950/40 p-4 hover:bg-zinc-950/60"
          href="/app/suggestions"
        >
          <div className="text-sm font-medium">Sugestões</div>
          <div className="mt-1 text-xs text-zinc-400">
            Liste pendências e aprove/rejeite com governança.
          </div>
        </Link>
      </div>
    </div>
  );
}


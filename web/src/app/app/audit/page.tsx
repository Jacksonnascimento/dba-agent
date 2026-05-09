"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { AuditLog, DatabaseConnection, Page } from "@/lib/types";

export default function AuditPage() {
  const [dbs, setDbs] = useState<DatabaseConnection[]>([]);
  const [dbId, setDbId] = useState<number | "all">("all");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadDbs() {
    const items = await apiFetch<DatabaseConnection[]>("/database-connections");
    setDbs(items.filter((d) => d.active));
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const q = new URLSearchParams();
      q.set("page", String(page));
      q.set("size", "20");
      if (dbId !== "all") q.set("databaseConnectionId", String(dbId));
      const res = await apiFetch<Page<AuditLog>>(`/audit/suggestions?${q}`);
      setData(res);
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar auditoria");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      await loadDbs();
      await load();
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dbId, page]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Auditoria</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Trilhas de aprovação, rejeição e execução pelo agente.
        </p>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-1">
            <div className="text-sm font-medium">Filtro por banco</div>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600 sm:w-[320px]"
              value={dbId}
              onChange={(e) => {
                setPage(0);
                setDbId(e.target.value === "all" ? "all" : Number(e.target.value));
              }}
            >
              <option value="all">Todos</option>
              {dbs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
            onClick={load}
            disabled={loading}
          >
            Recarregar
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        {loading ? (
          <div className="text-sm text-zinc-400">Carregando...</div>
        ) : !data || data.content.length === 0 ? (
          <div className="text-sm text-zinc-400">Sem eventos.</div>
        ) : (
          <div className="space-y-2">
            {data.content.map((l) => (
              <div
                key={l.id}
                className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"
              >
                <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm font-medium">
                    {l.action} • sugestão #{l.suggestionId}
                  </div>
                  <div className="text-xs text-zinc-500">
                    {new Date(l.createdAt).toLocaleString()}
                  </div>
                </div>
                <div className="mt-1 text-xs text-zinc-400">
                  {l.databaseConnectionName} • {l.actorType}{" "}
                  {l.actorIdentifier ? `(${l.actorIdentifier})` : ""}
                </div>
                {l.details ? (
                  <div className="mt-2 text-xs text-zinc-300">{l.details}</div>
                ) : null}
              </div>
            ))}
          </div>
        )}

        <div className="mt-4 flex items-center justify-between">
          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900 disabled:opacity-60"
            disabled={loading || !data || data.first}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Anterior
          </button>
          <div className="text-xs text-zinc-400">
            Página {data ? data.number + 1 : 1} de {data ? data.totalPages : 1}
          </div>
          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900 disabled:opacity-60"
            disabled={loading || !data || data.last}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima
          </button>
        </div>
      </section>
    </div>
  );
}


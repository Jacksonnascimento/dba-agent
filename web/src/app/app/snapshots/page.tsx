"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { DatabaseConnection, Page, Snapshot } from "@/lib/types";
import { CopyButton } from "@/components/CopyButton";

export default function SnapshotsPage() {
  const [dbs, setDbs] = useState<DatabaseConnection[]>([]);
  const [dbId, setDbId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Snapshot> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadDbs() {
    const items = await apiFetch<DatabaseConnection[]>("/database-connections");
    const active = items.filter((d) => d.active);
    setDbs(active);
    if (dbId == null && active.length > 0) setDbId(active[0]!.id);
  }

  async function load() {
    if (!dbId) return;
    setLoading(true);
    setError(null);
    try {
      const q = new URLSearchParams();
      q.set("page", String(page));
      q.set("size", "10");
      const res = await apiFetch<Page<Snapshot>>(
        `/database-connections/${dbId}/snapshots?${q}`
      );
      setData(res);
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar snapshots");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      await loadDbs();
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setPage(0);
  }, [dbId]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dbId, page]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Snapshots</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Histórico de telemetria rica por banco (DMVs, waits, top queries e
          planos).
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
            <div className="text-sm font-medium">Banco</div>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600 sm:w-[360px]"
              value={dbId ?? ""}
              onChange={(e) => setDbId(Number(e.target.value))}
              disabled={dbs.length === 0}
            >
              {dbs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.dbEngine})
                </option>
              ))}
            </select>
          </div>

          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
            onClick={load}
            disabled={loading || !dbId}
          >
            Recarregar
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        {loading ? (
          <div className="text-sm text-zinc-400">Carregando...</div>
        ) : !data || data.content.length === 0 ? (
          <div className="text-sm text-zinc-400">Sem snapshots.</div>
        ) : (
          <div className="space-y-3">
            {data.content.map((s) => (
              <div
                key={s.id}
                className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"
              >
                <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm font-medium">
                    Snapshot #{s.id} • {s.dbEngine}
                  </div>
                  <div className="text-xs text-zinc-500">
                    {new Date(s.collectedAt).toLocaleString()}
                  </div>
                </div>
                <div className="mt-2 grid gap-2 lg:grid-cols-2">
                  <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2 overflow-hidden">
                    <summary className="cursor-pointer text-xs text-zinc-200 flex justify-between items-center">
                      <span>Wait stats</span>
                      <CopyButton text={s.waitStats ?? ""} />
                    </summary>
                    <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
                      {s.waitStats ?? ""}
                    </pre>
                  </details>
                  <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2 overflow-hidden">
                    <summary className="cursor-pointer text-xs text-zinc-200 flex justify-between items-center">
                      <span>Top queries</span>
                      <CopyButton text={s.topQueries ?? ""} />
                    </summary>
                    <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
                      {s.topQueries ?? ""}
                    </pre>
                  </details>
                  <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2 lg:col-span-2 overflow-hidden">
                    <summary className="cursor-pointer text-xs text-zinc-200 flex justify-between items-center">
                      <span>Execution plans</span>
                      <CopyButton text={s.executionPlans ?? ""} />
                    </summary>
                    <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
                      {s.executionPlans ?? ""}
                    </pre>
                  </details>
                </div>
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


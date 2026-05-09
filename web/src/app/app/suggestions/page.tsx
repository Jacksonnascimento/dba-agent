"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { DatabaseConnection, Suggestion } from "@/lib/types";

export default function SuggestionsPage() {
  const [dbs, setDbs] = useState<DatabaseConnection[]>([]);
  const [dbId, setDbId] = useState<number | "all">("all");
  const [items, setItems] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const selectedDb = useMemo(() => {
    if (dbId === "all") return null;
    return dbs.find((d) => d.id === dbId) ?? null;
  }, [dbId, dbs]);

  async function loadDbs() {
    const data = await apiFetch<DatabaseConnection[]>("/database-connections");
    setDbs(data.filter((d) => d.active));
  }

  async function loadSuggestions() {
    setLoading(true);
    setError(null);
    try {
      const q =
        dbId === "all" ? "" : `?databaseConnectionId=${encodeURIComponent(dbId)}`;
      const data = await apiFetch<Suggestion[]>(`/suggestions/pending${q}`);
      setItems(data);
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar sugestões");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      try {
        await loadDbs();
      } catch {
        // ignore; handled by suggestions load
      }
      await loadSuggestions();
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadSuggestions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dbId]);

  async function approve(id: number) {
    setActionLoading(id);
    setError(null);
    try {
      await apiFetch(`/suggestions/${id}/approve`, { method: "POST" });
      await loadSuggestions();
    } catch (e: any) {
      setError(e?.message ?? "Falha ao aprovar");
    } finally {
      setActionLoading(null);
    }
  }

  async function reject(id: number) {
    setActionLoading(id);
    setError(null);
    try {
      await apiFetch(`/suggestions/${id}/reject`, { method: "POST" });
      await loadSuggestions();
    } catch (e: any) {
      setError(e?.message ?? "Falha ao rejeitar");
    } finally {
      setActionLoading(null);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Sugestões</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Pendências geradas pela telemetria do agente. Aprovar enfileira para
          execução.
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
              onChange={(e) =>
                setDbId(e.target.value === "all" ? "all" : Number(e.target.value))
              }
            >
              <option value="all">Todos</option>
              {dbs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.dbEngine})
                </option>
              ))}
            </select>
            {selectedDb ? (
              <div className="text-xs text-zinc-500">
                Exibindo pendências de: {selectedDb.name}
              </div>
            ) : null}
          </div>

          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
            onClick={loadSuggestions}
            disabled={loading}
          >
            Recarregar
          </button>
        </div>
      </section>

      <section className="space-y-3">
        {loading ? (
          <div className="text-sm text-zinc-400">Carregando...</div>
        ) : items.length === 0 ? (
          <div className="text-sm text-zinc-400">Nenhuma sugestão pendente.</div>
        ) : (
          items.map((s) => (
            <div
              key={s.id}
              className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4"
            >
              <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div className="text-sm font-medium">
                    #{s.id} • {s.databaseConnectionName}
                  </div>
                  <div className="mt-1 text-xs text-zinc-500">
                    status: {s.status} • criado em {new Date(s.createdAt).toLocaleString()}
                  </div>
                </div>
                <div className="flex gap-2">
                  <button
                    className="rounded-lg bg-zinc-50 text-zinc-900 px-3 py-2 text-xs font-medium hover:bg-white disabled:opacity-60"
                    disabled={actionLoading === s.id}
                    onClick={() => approve(s.id)}
                  >
                    Aprovar
                  </button>
                  <button
                    className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900 disabled:opacity-60"
                    disabled={actionLoading === s.id}
                    onClick={() => reject(s.id)}
                  >
                    Rejeitar
                  </button>
                </div>
              </div>

              <div className="mt-3 grid gap-3 lg:grid-cols-2">
                <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3">
                  <div className="text-xs text-zinc-400">Diagnóstico</div>
                  <pre className="mt-2 whitespace-pre-wrap text-xs text-zinc-200">
                    {s.suggestionText}
                  </pre>
                </div>
                <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3">
                  <div className="text-xs text-zinc-400">Up/Down scripts</div>
                  <div className="mt-2 space-y-2">
                    <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2">
                      <summary className="cursor-pointer text-xs text-zinc-200">
                        Up script
                      </summary>
                      <pre className="mt-2 whitespace-pre-wrap text-xs text-zinc-200">
                        {s.upScript}
                      </pre>
                    </details>
                    <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2">
                      <summary className="cursor-pointer text-xs text-zinc-200">
                        Down script
                      </summary>
                      <pre className="mt-2 whitespace-pre-wrap text-xs text-zinc-200">
                        {s.downScript}
                      </pre>
                    </details>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </section>
    </div>
  );
}


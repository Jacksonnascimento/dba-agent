"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { DatabaseConnection, Suggestion } from "@/lib/types";
import { CopyButton } from "@/components/CopyButton";

export default function SuggestionsPage() {
  const [dbs, setDbs] = useState<DatabaseConnection[]>([]);
  const [dbId, setDbId] = useState<number | "all">("all");
  const [activeTab, setActiveTab] = useState<"pending" | "executed">("pending");
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

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const q = dbId === "all" ? "" : `?databaseConnectionId=${encodeURIComponent(dbId)}`;
      const endpoint = activeTab === "pending" ? "/suggestions/pending" : "/suggestions/executed";
      const data = await apiFetch<Suggestion[]>(`${endpoint}${q}`);
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
      } catch {}
      await loadData();
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dbId, activeTab]);

  async function approve(id: number) {
    setActionLoading(id);
    setError(null);
    try {
      await apiFetch(`/suggestions/${id}/approve`, { method: "POST" });
      await loadData();
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
      await loadData();
    } catch (e: any) {
      setError(e?.message ?? "Falha ao rejeitar");
    } finally {
      setActionLoading(null);
    }
  }

  async function requestRollback(id: number) {
    if (!confirm("Tem certeza que deseja solicitar o Rollback? O agente executará o DownScript em breve.")) return;
    setActionLoading(id);
    setError(null);
    try {
      await apiFetch(`/suggestions/${id}/request-rollback`, { method: "POST" });
      await loadData();
    } catch (e: any) {
      setError(e?.message ?? "Falha ao solicitar rollback");
    } finally {
      setActionLoading(null);
    }
  }

  function getStatusBadge(status: string) {
    switch (status) {
      case "PENDING": return <span className="text-yellow-500">Pendente</span>;
      case "APPROVED": return <span className="text-blue-500">Aprovado (Na fila do agente)</span>;
      case "EXECUTED": return <span className="text-green-500">Executado com sucesso</span>;
      case "FAILED": return <span className="text-red-500">Falhou ao executar</span>;
      case "ROLLBACK_PENDING": return <span className="text-orange-500">Rollback na fila</span>;
      case "ROLLED_BACK": return <span className="text-purple-500">Desfeito com sucesso</span>;
      case "ROLLBACK_FAILED": return <span className="text-red-500">Falha ao desfazer</span>;
      default: return <span>{status}</span>;
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Sugestões</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Pendências geradas pela telemetria do agente e histórico de execuções.
        </p>
      </div>

      <div className="flex border-b border-zinc-800">
        <button
          className={`px-4 py-2 text-sm font-medium border-b-2 ${
            activeTab === "pending"
              ? "border-blue-500 text-blue-400"
              : "border-transparent text-zinc-400 hover:text-zinc-300"
          }`}
          onClick={() => setActiveTab("pending")}
        >
          Pendentes
        </button>
        <button
          className={`px-4 py-2 text-sm font-medium border-b-2 ${
            activeTab === "executed"
              ? "border-blue-500 text-blue-400"
              : "border-transparent text-zinc-400 hover:text-zinc-300"
          }`}
          onClick={() => setActiveTab("executed")}
        >
          Finalizadas / Executadas
        </button>
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
                Exibindo: {selectedDb.name}
              </div>
            ) : null}
          </div>

          <button
            className="rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
            onClick={loadData}
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
          <div className="text-sm text-zinc-400">Nenhum registro encontrado nesta aba.</div>
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
                    status: {getStatusBadge(s.status)} • criado em {new Date(s.createdAt).toLocaleString()}
                  </div>
                </div>
                <div className="flex gap-2">
                  {activeTab === "pending" && (
                    <>
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
                    </>
                  )}
                  {activeTab === "executed" && (s.status === "EXECUTED" || s.status === "ROLLBACK_FAILED") && (
                    <button
                      className="rounded-lg border border-red-900 text-red-400 bg-red-950/30 px-3 py-2 text-xs font-medium hover:bg-red-900/50 disabled:opacity-60"
                      disabled={actionLoading === s.id}
                      onClick={() => requestRollback(s.id)}
                    >
                      Desfazer (Rollback)
                    </button>
                  )}
                </div>
              </div>

              <div className="mt-3 grid gap-3 lg:grid-cols-2">
                <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 overflow-hidden">
                  <div className="text-xs text-zinc-400 flex justify-between items-center">
                    <span>Diagnóstico</span>
                    <CopyButton text={s.suggestionText ?? ""} />
                  </div>
                  <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
                    {s.suggestionText}
                  </pre>
                </div>
                <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 overflow-hidden">
                  <div className="text-xs text-zinc-400">Up/Down scripts</div>
                  <div className="mt-2 space-y-2">
                    <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2 overflow-hidden">
                      <summary className="cursor-pointer text-xs text-zinc-200 flex justify-between items-center">
                        <span>Up script</span>
                        <CopyButton text={s.upScript ?? ""} />
                      </summary>
                      <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
                        {s.upScript}
                      </pre>
                    </details>
                    <details className="rounded-lg border border-zinc-800 bg-zinc-900/30 p-2 overflow-hidden">
                      <summary className="cursor-pointer text-xs text-zinc-200 flex justify-between items-center">
                        <span>Down script</span>
                        <CopyButton text={s.downScript ?? ""} />
                      </summary>
                      <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all text-xs text-zinc-200">
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


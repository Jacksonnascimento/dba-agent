"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { AuditLog, DatabaseConnection, Page } from "@/lib/types";
import { ShieldAlert, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";

const ACTION_CONFIG: Record<string, { label: string; color: string }> = {
  APPROVED:            { label: "Aprovado",           color: "bg-green-500/10 text-green-400 border-green-500/20" },
  REJECTED:            { label: "Rejeitado",           color: "bg-red-500/10 text-red-400 border-red-500/20" },
  EXECUTED:            { label: "Executado",           color: "bg-blue-500/10 text-blue-400 border-blue-500/20" },
  EXECUTION_FAILED:    { label: "Falha na Execução",   color: "bg-orange-500/10 text-orange-400 border-orange-500/20" },
  ROLLBACK_REQUESTED:  { label: "Rollback Solicitado", color: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20" },
  ROLLED_BACK:         { label: "Revertido",           color: "bg-purple-500/10 text-purple-400 border-purple-500/20" },
  ROLLBACK_FAILED:     { label: "Falha no Rollback",   color: "bg-red-500/10 text-red-400 border-red-500/20" },
};

function ActionBadge({ action }: { action: string }) {
  const cfg = ACTION_CONFIG[action] ?? { label: action, color: "bg-zinc-500/10 text-zinc-400 border-zinc-500/20" };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${cfg.color}`}>
      {cfg.label}
    </span>
  );
}

function formatActor(type: string, identifier: string | null): string {
  if (type === "AGENT") return "Agente (automático)";
  if (type === "USER") {
    if (!identifier) return "Usuário";
    if (identifier.startsWith("user:")) {
      const parts = identifier.split(":");
      // formato novo: user:ID:Nome do Usuário
      if (parts.length >= 3) return parts.slice(2).join(":");
      // formato antigo: user:ID
      return `Usuário #${parts[1]}`;
    }
    return identifier;
  }
  return identifier ?? type;
}

export default function AuditPage() {
  const [dbs, setDbs] = useState<DatabaseConnection[]>([]);
  const [dbId, setDbId] = useState<number | "all">("all");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadDbs() {
    try {
      const items = await apiFetch<DatabaseConnection[]>("/database-connections");
      setDbs(items.filter((d) => d.active));
    } catch {
      // silently fail - filter will just be empty
    }
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const q = new URLSearchParams();
      q.set("page", String(page));
      q.set("size", "15");
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
    loadDbs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dbId, page]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-white flex items-center gap-2">
          <ShieldAlert className="w-6 h-6 text-blue-500" />
          Auditoria
        </h1>
        <p className="text-sm text-zinc-400 mt-1">
          Trilha completa de todas as aprovações, rejeições, execuções e reversões feitas pelo sistema.
        </p>
      </div>

      {/* Filters */}
      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Filtrar por banco de dados</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm text-white outline-none focus:ring-2 focus:ring-blue-500/50 sm:w-72"
              value={dbId}
              onChange={(e) => {
                setPage(0);
                setDbId(e.target.value === "all" ? "all" : Number(e.target.value));
              }}
            >
              <option value="all">Todos os bancos</option>
              {dbs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          <button
            className="flex items-center gap-2 rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-300 hover:text-white hover:bg-zinc-800 transition-colors disabled:opacity-50"
            onClick={load}
            disabled={loading}
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Atualizar
          </button>
        </div>
      </section>

      {/* Error */}
      {error && (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-200">
          {error}
        </div>
      )}

      {/* Log table */}
      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        {loading ? (
          <div className="flex items-center justify-center py-12 text-zinc-500 text-sm gap-2">
            <RefreshCw className="w-4 h-4 animate-spin" />
            Carregando eventos...
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-zinc-500 gap-2">
            <ShieldAlert className="w-8 h-8 text-zinc-700" />
            <p className="text-sm">Nenhum evento de auditoria registrado.</p>
            <p className="text-xs text-zinc-600">Os eventos aparecem quando sugestões são aprovadas, rejeitadas ou executadas.</p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-zinc-800">
            <table className="w-full text-sm">
              <thead className="bg-zinc-950/60 text-zinc-300">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Evento</th>
                  <th className="text-left px-4 py-3 font-medium">Banco de Dados</th>
                  <th className="text-left px-4 py-3 font-medium">Sugestão</th>
                  <th className="text-left px-4 py-3 font-medium">Executado por</th>
                  <th className="text-left px-4 py-3 font-medium">Detalhes</th>
                  <th className="text-right px-4 py-3 font-medium">Data/Hora</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60">
                {data.content.map((l) => (
                  <tr key={l.id} className="hover:bg-zinc-800/20 transition-colors">
                    <td className="px-4 py-3">
                      <ActionBadge action={l.action} />
                    </td>
                    <td className="px-4 py-3 text-zinc-300">
                      {l.databaseConnectionName}
                    </td>
                    <td className="px-4 py-3 text-zinc-400 text-xs">
                      #{l.suggestionId}
                    </td>
                    <td className="px-4 py-3 text-zinc-400 text-xs">
                      {formatActor(l.actorType, l.actorIdentifier)}
                    </td>
                    <td className="px-4 py-3 text-zinc-400 text-xs max-w-xs truncate">
                      {l.details ?? <span className="text-zinc-600">—</span>}
                    </td>
                    <td className="px-4 py-3 text-right text-zinc-500 text-xs whitespace-nowrap">
                      {new Date(l.createdAt).toLocaleString("pt-BR")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between">
            <button
              className="flex items-center gap-1 rounded-lg border border-zinc-800 px-3 py-1.5 text-xs text-zinc-300 hover:bg-zinc-900 disabled:opacity-40 transition-colors"
              disabled={loading || data.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              <ChevronLeft className="w-3.5 h-3.5" />
              Anterior
            </button>
            <span className="text-xs text-zinc-500">
              Página {data.number + 1} de {data.totalPages}
              <span className="ml-2 text-zinc-600">({data.totalElements} eventos)</span>
            </span>
            <button
              className="flex items-center gap-1 rounded-lg border border-zinc-800 px-3 py-1.5 text-xs text-zinc-300 hover:bg-zinc-900 disabled:opacity-40 transition-colors"
              disabled={loading || data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Próxima
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </section>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { Server, Plus, Edit, Zap, Download } from 'lucide-react';
import { AiPromptFields } from "@/components/AiPromptFields";
import { toast } from 'sonner';
import { getToken } from '@/lib/auth-storage';
import type { DatabaseConnection } from "@/lib/types";

export interface AgentWorker {
  id: number;
  name: string;
  workerToken: string;
  snapshotIntervalMinutes: number;
  aiInstructionsAddon?: string | null;
  databases: DatabaseConnection[];
  createdAt: string;
}

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentWorker[]>([]);
  const [databases, setDatabases] = useState<DatabaseConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [interval, setInterval] = useState<number>(1440);
  const [selectedDbs, setSelectedDbs] = useState<number[]>([]);
  const [aiInstructionsAddon, setAiInstructionsAddon] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    setLoading(true);
    try {
      const [agentsData, dbsData] = await Promise.all([
        apiFetch<AgentWorker[]>("/agent-workers"),
        apiFetch<DatabaseConnection[]>("/database-connections")
      ]);
      setAgents(agentsData);
      setDatabases(dbsData.filter(d => d.active));
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar dados");
    } finally {
      setLoading(false);
    }
  }

  const toggleDbSelection = (dbId: number) => {
    setSelectedDbs(prev => 
      prev.includes(dbId) ? prev.filter(id => id !== dbId) : [...prev, dbId]
    );
  };

  const handleEdit = (agent: AgentWorker) => {
    setEditingId(agent.id);
    setName(agent.name);
    setInterval(agent.snapshotIntervalMinutes);
    setSelectedDbs(agent.databases.map(db => db.id));
    setAiInstructionsAddon(agent.aiInstructionsAddon ?? "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const resetForm = () => {
    setEditingId(null);
    setName("");
    setInterval(1440);
    setSelectedDbs([]);
    setAiInstructionsAddon("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const payload = {
        name,
        snapshotIntervalMinutes: interval,
        databaseConnectionIds: selectedDbs,
        aiInstructionsAddon: aiInstructionsAddon.trim() || null,
      };

      if (editingId) {
        await apiFetch(`/agent-workers/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
        toast.success("Agente atualizado!");
      } else {
        await apiFetch("/agent-workers", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        toast.success("Agente criado!");
      }
      resetForm();
      fetchData();
    } catch (e: any) {
      toast.error(e?.message ?? "Falha ao salvar");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleForceTelemetry = async (id: number) => {
    if (!confirm("Isso fará com que o Agente extraia dados de todos os bancos vinculados imediatamente. Deseja continuar?")) return;
    try {
      await apiFetch(`/agent-workers/${id}/force-telemetry`, { method: "POST" });
      toast.success("Ordem enviada com sucesso! O Agente iniciará a extração em até 5 segundos.");
    } catch (e: any) {
      toast.error(e?.message ?? "Falha ao forçar coleta");
    }
  };

  const handleDownloadAgent = async (id: number, os: string) => {
    try {
      const token = getToken();
      // Correção: Utilizando a variável de ambiente em vez do localhost fixo
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
      const url = `${baseUrl}/agent-workers/${id}/agent-bundle?os=${os}`;
      
      const res = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/zip",
        },
      });
      
      if (!res.ok) throw new Error("Erro na geração do ZIP");
      
      const blob = await res.blob();
      const a = document.createElement("a");
      a.href = window.URL.createObjectURL(blob);
      a.download = `dba-agent-${os}-bundle.zip`;
      a.click();
      toast.success(`Pacote para ${os} gerado com sucesso!`);
    } catch (e) {
      toast.error("Erro ao gerar pacote. O backend foi reiniciado?");
    }
  };

  const copyToken = (token: string) => {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(token);
      toast.success("Token Master copiado!");
    } else {
      const textArea = document.createElement("textarea");
      textArea.value = token;
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        toast.success("Token Master copiado!");
      } catch (err) {
        toast.error("Navegador não suporta cópia automática.");
      }
      document.body.removeChild(textArea);
    }
  };

  if (loading) return <div className="p-4 text-white">Carregando...</div>;
  if (error) return <div className="p-4 text-red-500">{error}</div>;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-white flex items-center gap-2">
          <Server className="w-6 h-6 text-blue-500" />
          Gerenciamento de Agentes (Workers)
        </h1>
        <p className="text-sm text-zinc-400 mt-1">
          Crie perfis de servidores, defina o intervalo de coleta e escolha quais bancos cada agente irá monitorar.
        </p>
      </div>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <h2 className="text-lg font-medium text-white mb-4 flex items-center gap-2">
          {editingId ? <><Edit className="w-5 h-5 text-blue-400" /> Editar Agente</> : <><Plus className="w-5 h-5 text-blue-400" /> Cadastrar Novo Agente</>}
        </h2>
        
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-white/70">Nome do Agente (Servidor)</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                placeholder="Ex: SRV-PROD-SQL-01"
                required
              />
            </div>
            
            <div className="space-y-2">
              <label className="text-sm font-medium text-white/70">Intervalo Mestre de Extração</label>
              <select
                value={interval}
                onChange={(e) => setInterval(parseInt(e.target.value))}
                className="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                required
              >
                <option value={5} className="bg-[#0f172a]">5 minutos (Testes)</option>
                <option value={60} className="bg-[#0f172a]">1 hora</option>
                <option value={720} className="bg-[#0f172a]">12 horas</option>
                <option value={1440} className="bg-[#0f172a]">24 horas (Recomendado)</option>
              </select>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-white/70">Bancos Associados a este Agente</label>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
              {databases.map(db => (
                <label key={db.id} className="flex items-center gap-3 p-3 rounded-lg border border-zinc-800 bg-zinc-900/50 cursor-pointer hover:bg-zinc-800/50 transition-colors">
                  <input 
                    type="checkbox"
                    checked={selectedDbs.includes(db.id)}
                    onChange={() => toggleDbSelection(db.id)}
                    className="w-4 h-4 text-blue-500 rounded border-zinc-700 bg-zinc-800 focus:ring-blue-500 focus:ring-offset-zinc-900"
                  />
                  <span className="text-sm text-zinc-300">{db.name}</span>
                </label>
              ))}
              {databases.length === 0 && (
                <p className="text-sm text-zinc-500">Nenhum banco de dados ativo cadastrado.</p>
              )}
            </div>
          </div>

          <AiPromptFields
            dbEngine={
              selectedDbs.length > 0
                ? databases.find((d) => d.id === selectedDbs[0])?.dbEngine ?? "SQL Server"
                : "SQL Server"
            }
            addon={aiInstructionsAddon}
            onAddonChange={setAiInstructionsAddon}
            agentWorkerId={editingId}
            databaseConnectionId={selectedDbs[0] ?? null}
            bankLabel="agente"
          />

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors disabled:opacity-50"
            >
              {isSubmitting ? "Salvando..." : editingId ? "Salvar Alterações" : "Criar Agente"}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={resetForm}
                className="px-6 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-white font-medium rounded-lg transition-colors"
              >
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium mb-3">Agentes Configurados</div>
        <div className="overflow-hidden rounded-xl border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60 text-zinc-300">
              <tr>
                <th className="text-left px-4 py-3 font-medium">Nome (Servidor)</th>
                <th className="text-left px-4 py-3 font-medium">Bancos Monitorados</th>
                <th className="text-left px-4 py-3 font-medium">Token Mestre</th>
                <th className="text-right px-4 py-3 font-medium">Ações / Instalação</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {agents.length === 0 ? (
                <tr>
                  <td className="px-4 py-4 text-center text-zinc-500" colSpan={4}>Nenhum agente configurado.</td>
                </tr>
              ) : (
                agents.map((agent) => (
                  <tr key={agent.id} className="hover:bg-zinc-800/20 transition-colors">
                    <td className="px-4 py-3 text-white font-medium">
                      {agent.name}
                      <span className="block text-xs text-zinc-500 mt-1">
                        Cada {agent.snapshotIntervalMinutes === 1440 ? "24h" : agent.snapshotIntervalMinutes === 60 ? "1h" : agent.snapshotIntervalMinutes + "m"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-zinc-400">
                      {agent.databases.length === 0 ? "Nenhum banco" : agent.databases.map(db => db.name).join(", ")}
                    </td>
                    <td className="px-4 py-3">
                      <button 
                        onClick={() => copyToken(agent.workerToken)}
                        className="text-xs font-mono bg-zinc-900 border border-zinc-700 rounded px-2 py-1 text-zinc-300 hover:text-white hover:border-zinc-500 transition-colors"
                        title="Clique para copiar"
                      >
                        {agent.workerToken.substring(0, 12)}...
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleEdit(agent)}
                          className="p-2 text-white/50 hover:text-blue-400 hover:bg-blue-500/10 rounded-lg transition-colors"
                          title="Editar"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleForceTelemetry(agent.id)}
                          className="p-2 text-white/50 hover:text-yellow-400 hover:bg-yellow-500/10 rounded-lg transition-colors"
                          title="Forçar Coleta (Todos os bancos deste agente)"
                        >
                          <Zap className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDownloadAgent(agent.id, 'windows')}
                          className="p-2 text-white/50 hover:text-green-400 hover:bg-green-500/10 rounded-lg transition-colors flex items-center gap-1 text-xs"
                          title="Baixar Instalador Windows"
                        >
                          <Download className="w-4 h-4" />
                          <span className="hidden lg:inline">Win</span>
                        </button>
                        <button
                          onClick={() => handleDownloadAgent(agent.id, 'linux')}
                          className="p-2 text-white/50 hover:text-green-400 hover:bg-green-500/10 rounded-lg transition-colors flex items-center gap-1 text-xs"
                          title="Baixar Instalador Linux"
                        >
                          <Download className="w-4 h-4" />
                          <span className="hidden lg:inline">Lin</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
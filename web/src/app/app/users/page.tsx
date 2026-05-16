"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import { toast } from "sonner";

type Tenant = {
  id: number;
  name: string;
  active: boolean;
};

type User = {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean;
  tenantId?: number;
  tenantName?: string;
  createdAt: string;
};

export default function UsersPage() {
  const { user, isAdmin } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [isCreating, setIsCreating] = useState(false);
  const [formName, setFormName] = useState("");
  const [formEmail, setFormEmail] = useState("");
  const [formPassword, setFormPassword] = useState("");
  const [formRole, setFormRole] = useState("ROLE_CLIENT");
  const [formTenantId, setFormTenantId] = useState<string>("");

  // Change password states
  const [changePasswordUserId, setChangePasswordUserId] = useState<number | null>(null);
  const [newPassword, setNewPassword] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  async function fetchData() {
    if (!isAdmin) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [usersData, tenantsData] = await Promise.all([
        apiFetch<User[]>("/users"),
        apiFetch<Tenant[]>("/tenants"),
      ]);
      setUsers(usersData);
      setTenants(tenantsData.filter((t) => t.active));
      if (tenantsData.length > 0) {
        setFormTenantId(String(tenantsData[0].id));
      }
    } catch (e: any) {
      setError(e?.message || "Falha ao carregar dados");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  async function handleCreateUser(e: React.FormEvent) {
    e.preventDefault();
    setIsCreating(true);
    setError(null);
    try {
      await apiFetch("/users", {
        method: "POST",
        body: JSON.stringify({
          name: formName,
          email: formEmail,
          password: formPassword,
          role: formRole,
          tenantId: formTenantId ? Number(formTenantId) : null,
        }),
      });
      setFormName("");
      setFormEmail("");
      setFormPassword("");
      setFormRole("ROLE_CLIENT");
      toast.success("Usuário criado com sucesso!");
      fetchData();
    } catch (e: any) {
      toast.error(e?.message || "Falha ao criar usuário");
    } finally {
      setIsCreating(false);
    }
  }

  async function handleChangePassword(e: React.FormEvent) {
    e.preventDefault();
    if (!changePasswordUserId) return;
    setChangingPassword(true);
    setError(null);
    try {
      await apiFetch(`/users/${changePasswordUserId}/password`, {
        method: "PUT",
        body: JSON.stringify({ password: newPassword }),
      });
      toast.success("Senha alterada com sucesso!");
      setChangePasswordUserId(null);
      setNewPassword("");
    } catch (e: any) {
      toast.error(e?.message || "Falha ao alterar senha");
    } finally {
      setChangingPassword(false);
    }
  }

  async function handleToggleActive(u: User) {
    try {
      await apiFetch(`/users/${u.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: u.name,
          email: u.email,
          role: u.role,
          active: !u.active,
        }),
      });
      toast.success(`Usuário ${!u.active ? "ativado" : "desativado"} com sucesso!`);
      fetchData();
    } catch (e: any) {
      toast.error(e?.message || "Falha ao alterar status do usuário");
    }
  }

  // Usuário comum - Só pode trocar a própria senha
  if (!isAdmin) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Meus Dados</h1>
          <p className="mt-1 text-sm text-zinc-400">
            Você está logado como {user?.name} ({user?.email})
          </p>
        </div>

        <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4 max-w-md">
          <div className="text-sm font-medium mb-4">Alterar Minha Senha</div>
          <form
            className="space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                await apiFetch("/users/me/password", {
                  method: "PUT",
                  body: JSON.stringify({ password: newPassword }),
                });
                toast.success("Senha alterada com sucesso!");
                setNewPassword("");
              } catch (err: any) {
                toast.error(err?.message || "Falha ao alterar senha");
              }
            }}
          >
            <div className="space-y-1">
              <label className="text-xs text-zinc-400">Nova Senha</label>
              <input
                type="password"
                className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={4}
              />
            </div>
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              type="submit"
            >
              Alterar Senha
            </button>
          </form>
        </section>
      </div>
    );
  }

  // Admin - Pode ver tudo
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Usuários</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Gerencie os acessos do sistema. Cada usuário pertence a uma empresa (tenant).
        </p>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      {/* Aviso se não há tenants cadastrados */}
      {!loading && tenants.length === 0 && (
        <div className="rounded-lg border border-yellow-800 bg-yellow-950/30 px-4 py-3 text-sm text-yellow-200">
          ⚠️ Nenhuma empresa ativa cadastrada. Cadastre uma empresa em{" "}
          <a href="/app/tenants" className="underline hover:text-yellow-100">
            Empresas (Tenants)
          </a>{" "}
          antes de criar usuários.
        </div>
      )}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium mb-4">Novo Usuário</div>
        <form className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3" onSubmit={handleCreateUser}>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Nome</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Email (Login)</label>
            <input
              type="text"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={formEmail}
              onChange={(e) => setFormEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Senha Inicial</label>
            <input
              type="password"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={formPassword}
              onChange={(e) => setFormPassword(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Perfil</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={formRole}
              onChange={(e) => setFormRole(e.target.value)}
            >
              <option value="ROLE_CLIENT">Comum (Cliente)</option>
              <option value="ROLE_ADMIN">Administrador</option>
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Empresa (Tenant)</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={formTenantId}
              onChange={(e) => setFormTenantId(e.target.value)}
              required
            >
              <option value="">Selecione uma empresa...</option>
              {tenants.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-end">
            <button
              className="w-full rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={isCreating || tenants.length === 0}
              type="submit"
            >
              {isCreating ? "Criando..." : "Salvar Usuário"}
            </button>
          </div>
        </form>
      </section>

      {changePasswordUserId ? (
        <section className="rounded-2xl border border-yellow-900/50 bg-zinc-950/30 p-4">
          <div className="text-sm font-medium text-yellow-500 mb-4">
            Alterar senha do usuário #{changePasswordUserId}
          </div>
          <form className="flex gap-4 items-end" onSubmit={handleChangePassword}>
            <div className="space-y-1 flex-1">
              <label className="text-xs text-zinc-400">Nova Senha</label>
              <input
                type="password"
                className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </div>
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={changingPassword}
              type="submit"
            >
              Confirmar
            </button>
            <button
              type="button"
              className="rounded-lg border border-zinc-800 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-900"
              onClick={() => setChangePasswordUserId(null)}
            >
              Cancelar
            </button>
          </form>
        </section>
      ) : null}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium mb-3">Lista de Usuários</div>
        <div className="overflow-hidden rounded-xl border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60 text-zinc-300">
              <tr>
                <th className="text-left px-3 py-2 font-medium">ID</th>
                <th className="text-left px-3 py-2 font-medium">Nome</th>
                <th className="text-left px-3 py-2 font-medium">Email</th>
                <th className="text-left px-3 py-2 font-medium">Empresa</th>
                <th className="text-left px-3 py-2 font-medium">Perfil</th>
                <th className="text-left px-3 py-2 font-medium">Status</th>
                <th className="text-left px-3 py-2 font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {loading ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={7}>
                    Carregando...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={7}>
                    Nenhum usuário encontrado.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-zinc-800/20 transition-colors">
                    <td className="px-3 py-2 text-zinc-400">#{u.id}</td>
                    <td className="px-3 py-2 text-white">{u.name}</td>
                    <td className="px-3 py-2 text-zinc-300">{u.email}</td>
                    <td className="px-3 py-2 text-zinc-300">
                      {u.tenantName ? (
                        <span className="inline-flex items-center gap-1 text-xs bg-zinc-800 border border-zinc-700 rounded-full px-2 py-0.5">
                          {u.tenantName}
                        </span>
                      ) : (
                        <span className="text-zinc-600 text-xs">—</span>
                      )}
                    </td>
                    <td className="px-3 py-2">
                      <span
                        className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          u.role === "ROLE_ADMIN"
                            ? "bg-purple-500/10 text-purple-400"
                            : "bg-blue-500/10 text-blue-400"
                        }`}
                      >
                        {u.role === "ROLE_ADMIN" ? "Admin" : "Cliente"}
                      </span>
                    </td>
                    <td className="px-3 py-2">
                      <span
                        className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          u.active
                            ? "bg-green-500/10 text-green-400"
                            : "bg-red-500/10 text-red-400"
                        }`}
                      >
                        {u.active ? "Ativo" : "Inativo"}
                      </span>
                    </td>
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-2">
                        <button
                          className="text-xs text-blue-400 hover:text-blue-300 transition-colors"
                          onClick={() => setChangePasswordUserId(u.id)}
                        >
                          Mudar Senha
                        </button>
                        <span className="text-zinc-700">|</span>
                        <button
                          className={`text-xs transition-colors ${
                            u.active
                              ? "text-red-400 hover:text-red-300"
                              : "text-green-400 hover:text-green-300"
                          }`}
                          onClick={() => handleToggleActive(u)}
                        >
                          {u.active ? "Desativar" : "Ativar"}
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

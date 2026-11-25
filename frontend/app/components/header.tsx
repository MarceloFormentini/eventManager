export default function Header() {
  return (
    <header className="bg-[#00A651] text-white shadow-lg">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center">
              <span className="text-[#00A651] font-bold text-xl">C</span>
            </div>
            <div>
              <h1 className="text-xl font-bold">Sistema de Gestão</h1>
              <p className="text-green-100 text-sm">Gestão Institucional</p>
            </div>
          </div>
          <div className="text-right">
            <p className="text-sm text-green-100">Bem-vindo ao sistema</p>
            <p className="text-xs text-green-200">Versão 1.0</p>
          </div>
        </div>
      </div>
    </header>
  )
}

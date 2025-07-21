import { Button } from "@/components/ui/button"
import { Routes, Route, useNavigate } from 'react-router-dom'
import BattlePage from './battle/BattlePage'

function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-svh flex-col items-center justify-center">
      <Button onClick={() => navigate('/battle')}>Começar batalha</Button>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/battle" element={<BattlePage />} />
    </Routes>
  )
}

export default App
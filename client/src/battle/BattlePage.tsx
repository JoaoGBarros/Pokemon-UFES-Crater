import { Button } from "@/components/ui/button"
import { useNavigate } from 'react-router-dom'
import { useEffect, useState, useContext } from 'react'
import battleBackground from '@/assets/battle_background/Game Boy Advance - Pokemon FireRed LeafGreen - Battle Backgrounds.png'
import { WebSocketContext } from "@/WebSocketContext"

interface Pokemon {
    name: string
    level: number
    currentHp: number
    maxHp: number
    sprite: string
    moves: string[]
}

function BattlePage() {
    const navigate = useNavigate()
    const [selectedAction, setSelectedAction] = useState<string | null>(null)
    const [playerPokemon, setPlayerPokemon] = useState<Pokemon>()
    const [enemyPokemon, setEnemyPokemon] = useState<Pokemon>()
    const [inProgress, setInProgress] = useState<boolean>(false)
    const [battleLog, setBattleLog] = useState<string[]>([])
    const socket = useContext(WebSocketContext)

    useEffect(() => {
        if (socket && socket.current) {
            // Define o message handler específico para a página de batalha
            socket.current.onmessage = (event) => {
                const serverMessage = JSON.parse(event.data);
                console.log("BattlePage received message: ", serverMessage); // Log para depuração

                switch (serverMessage.type) {
                    case "battleStart":
                    case "battleState":
                        if (serverMessage.payload.battleStatus === "BATTLE_ENDED") {
                            setInProgress(false);
                        } else {
                            setInProgress(true);
                        }
                        setPlayerPokemon({
                            ...serverMessage.payload.player,
                            maxHp: serverMessage.payload.player.stats?.hp ?? serverMessage.payload.player.currentHp
                        });
                        setEnemyPokemon({
                            ...serverMessage.payload.enemy,
                            maxHp: serverMessage.payload.enemy.stats?.hp ?? serverMessage.payload.enemy.currentHp
                        });
                        if (Array.isArray(serverMessage.payload.chatMessage)) {
                            setBattleLog(serverMessage.payload.chatMessage);
                        }
                        break;
                    case "chatMessage":
                         if (Array.isArray(serverMessage.payload)) {
                            setBattleLog(serverMessage.payload);
                        }
                        break;
                }
            };

            // Inicia a batalha
            socket.current.send(JSON.stringify({ type: 'startRandomBattle' }));
        }
        
        // Função de limpeza para remover o handler quando o componente desmontar
        return () => {
            if (socket && socket.current) {
                socket.current.onmessage = null;
            }
        }
    }, [socket]);


    const handleAttack = (attack: string) => {
        if(socket && socket.current){
            socket.current.send(JSON.stringify({
                type: "battleCommand",
                payload: ["attack", attack],
            }));
            setSelectedAction(null); 
        }
    };

    const handleAction = (action: string) => {
        setSelectedAction(action)
    }

    const handleChatSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const input = e.currentTarget.elements.namedItem('chatInput') as HTMLInputElement;
        const message = input.value.trim();

        if(message && socket && socket.current) {
            socket.current.send(JSON.stringify({
                type: 'batteChat', // Usa o tipo de chat de batalha
                payload: message
            }));
        }
        input.value = '';
    };

    const getHpPercentage = (hp: number, maxHp: number) => {
        if (!maxHp) return 0;
        return (hp / maxHp) * 100
    }

    const getHpColor = (percentage: number) => {
        if (percentage > 50) return 'bg-green-500'
        if (percentage > 25) return 'bg-yellow-500'
        return 'bg-red-500'
    }

    return (
        <div className="h-screen w-full relative overflow-hidden">
            <div className="relative z-10 h-full flex flex-col">
                <div className="flex-1 flex flex-col" style={{ backgroundImage: `url(${battleBackground})`, backgroundRepeat: 'no-repeat', backgroundPosition: 'center 60%', backgroundSize: 'contain', backgroundColor: 'rgb(74, 144, 226)' }}>
                    <div className="flex-1 flex items-start justify-end p-8 mt-10">
                        <div className="text-right flex flex-row-reverse">
                            <div className="bg-white bg-opacity-90 rounded-lg p-3 mb-4 shadow-lg">
                                <div className="font-bold text-lg">{enemyPokemon?.name ?? "???"}</div>
                                <div className="text-sm text-gray-600">Nv. {enemyPokemon?.level ?? "???"}</div>
                                <div className="mt-2">
                                    <div className="text-xs mb-1">HP</div>
                                    <div className="w-32 bg-gray-200 rounded-full h-2">
                                        <div
                                            className={`h-2 rounded-full transition-all duration-300 ${getHpColor(getHpPercentage(enemyPokemon?.currentHp ?? 0, enemyPokemon?.maxHp ?? 1))}`}
                                            style={{ width: `${getHpPercentage(enemyPokemon?.currentHp ?? 0, enemyPokemon?.maxHp ?? 1)}%` }}
                                        ></div>
                                    </div>
                                    <div className="text-xs text-right mt-1">{enemyPokemon?.currentHp ?? "?"}/{enemyPokemon?.maxHp ?? "?"}</div>
                                </div>
                            </div>
                            <img
                                src={enemyPokemon?.sprite}
                                alt={enemyPokemon?.name}
                                className="w-45 h-45 object-contain relative"
                                style={{ transform: 'translateY(150px) translateX(-400px)' }}
                            />
                        </div>
                    </div>

                    <div className="flex-1 flex items-end justify-start p-8 mt-10">
                        <div className="text-left flex flex-row-reverse flex-start">
                            <img
                                src={playerPokemon?.sprite}
                                alt={playerPokemon?.name ?? ""}
                                className="w-50 h-50 object-contain relative"
                                style={{ transform: 'translateY(50px) translateX(300px)' }}
                            />
                            <div className="bg-white bg-opacity-90 rounded-lg p-3 shadow-lg">
                                <div className="font-bold text-lg">{playerPokemon?.name}</div>
                                <div className="text-sm text-gray-600">Nv. {playerPokemon?.level}</div>
                                <div className="mt-2">
                                    <div className="text-xs mb-1">HP</div>
                                    <div className="w-40 bg-gray-200 rounded-full h-3">
                                        <div
                                            className={`h-3 rounded-full transition-all duration-300 ${getHpColor(getHpPercentage(playerPokemon?.currentHp ?? 0, playerPokemon?.maxHp ?? 1))}`}
                                            style={{ width: `${getHpPercentage(playerPokemon?.currentHp ?? 0, playerPokemon?.maxHp ?? 1)}%` }}
                                        ></div>
                                    </div>
                                    <div className="text-xs mt-1">{playerPokemon?.currentHp}/{playerPokemon?.maxHp}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="bg-white bg-opacity-95 border-t-4 border-blue-600 p-6">
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <div className="order-2 lg:order-1">
                            <div className="flex flex-col gap-2">
                                <div className="bg-gray-100 rounded-lg p-4 h-32 overflow-y-auto">
                                    <div className="text-sm space-y-1">
                                        {battleLog.map((log, index) => (
                                            <div key={index} className="text-gray-800">{log}</div>
                                        ))}
                                    </div>
                                </div>
                                <form className="flex" onSubmit={handleChatSubmit}>
                                    <input
                                        type="text"
                                        name="chatInput"
                                        placeholder="Digite sua mensagem..."
                                        className="flex-1 rounded-l-lg border border-gray-300 px-3 py-2 focus:outline-none"
                                        autoComplete="off"
                                    />
                                    <button
                                        type="submit"
                                        className="rounded-r-lg bg-blue-500 text-white px-4 py-2 font-semibold hover:bg-blue-600"
                                    >Enviar</button>
                                </form>
                                {!inProgress ?
                                    <Button
                                        onClick={() => navigate("/game")}
                                        variant="outline"
                                        className="w-full"
                                    >
                                        ← Voltar para o mapa
                                    </Button>
                                    : null}
                            </div>
                        </div>
                        <div className="order-1 lg:order-2" style={{ display: inProgress ? 'block' : 'none' }}>
                            {!selectedAction ? (
                                <div className="grid grid-cols-2 gap-3">
                                    <Button
                                        onClick={() => handleAction('LUTA')}
                                        className="h-16 text-lg font-semibold bg-red-500 hover:bg-red-600"
                                    >
                                        ⚔️ LUTA
                                    </Button>
                                    <Button
                                        disabled={true}
                                        onClick={() => handleAction('BOLSA')}
                                        className="h-16 text-lg font-semibold bg-blue-500 hover:bg-blue-600"
                                    >
                                        🎒 BOLSA
                                    </Button>
                                    <Button
                                        disabled={true}
                                        onClick={() => handleAction('POKÉMON')}
                                        className="h-16 text-lg font-semibold bg-green-500 hover:bg-green-600"
                                    >
                                        ⚡ POKÉMON
                                    </Button>
                                    <Button
                                        onClick={() => navigate('/game')}
                                        className="h-16 text-lg font-semibold bg-gray-500 hover:bg-gray-600"
                                    >
                                        🏃 FUGIR
                                    </Button>
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    <div className="text-lg font-semibold mb-3">Escolha um ataque:</div>
                                    <div className="grid grid-cols-2 gap-2">
                                        {(playerPokemon?.moves ?? []).map((move) => (
                                            <Button key={move} onClick={() => handleAttack(move)} className="bg-yellow-500 hover:bg-yellow-600">
                                                {move}
                                            </Button>
                                        ))}
                                    </div>
                                    <Button
                                        onClick={() => setSelectedAction(null)}
                                        variant="outline"
                                        className="w-full"
                                    >
                                        ← Voltar
                                    </Button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div >
    )
}

export default BattlePage

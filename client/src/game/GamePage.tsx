import React, { useContext, useEffect, useState, useRef } from 'react';
import { WebSocketContext } from '@/WebSocketContext';
import { Button } from "@/components/ui/button";
import { useNavigate } from 'react-router-dom';
import mapBackground from '@/assets/Mapa0.png'; // Importe a imagem do mapa

// Interface para o objeto Player
interface Player {
    id: number;
    nickname: string;
    posX: number;
    posY: number;
}

// Constantes do Mapa e Jogo
const TILE_SIZE = 32;
const MAP_WIDTH_TILES = 20;
const MAP_HEIGHT_TILES = 40;
// Componente para renderizar um único jogador
const PlayerSprite: React.FC<{ player: Player }> = ({ player }) => (
    <div
        key={player.id}
        style={{
            position: 'absolute',
            left: `${player.posX * TILE_SIZE}px`,
            top: `${player.posY * TILE_SIZE}px`,
            width: TILE_SIZE,
            height: TILE_SIZE,
            backgroundColor: 'red',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'left 0.1s linear, top 0.1s linear',
            zIndex: 1,
            border: '2px solid black',
            boxSizing: 'border-box',
            fontWeight: 'bold',
        }}
    >
        {player.nickname.charAt(0).toUpperCase()}
    </div>
);

// Componente do Mapa
const Map: React.FC<{ players: Player[] }> = ({ players }) => (
    <div style={{
        position: 'relative',
        width: `${MAP_WIDTH_TILES * TILE_SIZE}px`,
        height: `${MAP_HEIGHT_TILES * TILE_SIZE}px`,
        border: '2px solid black',
        // Use a imagem importada como fundo
        backgroundImage: `url(${mapBackground})`,
        backgroundSize: 'cover', // Garante que a imagem cubra toda a área
        overflow: 'hidden'
    }}>
        {players.map(player => <PlayerSprite key={player.id} player={player} />)}
    </div>
);

// Componente do Chat
const Chat: React.FC<{ messages: string[], onSendMessage: (msg: string) => void }> = ({ messages, onSendMessage }) => {
    const [input, setInput] = useState("");
    const chatEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (input.trim()) {
            onSendMessage(input);
            setInput("");
        }
    };

    return (
        <div className="w-80 flex flex-col border rounded-lg p-4 bg-white shadow h-[500px]">
            <h2 className="font-bold mb-2 text-lg">Chat Global</h2>
            <div className="flex-1 bg-gray-100 rounded-lg p-2 overflow-y-auto mb-2">
                <div className="text-sm space-y-1">
                    {messages.map((message, index) => (
                        <div key={index} className="text-gray-800 break-words">{message}</div>
                    ))}
                    <div ref={chatEndRef} />
                </div>
            </div>
            <form onSubmit={handleSubmit} className="flex gap-2">
                <input
                    className="flex-1 border rounded px-2 py-1"
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    placeholder="Digite sua mensagem..."
                />
                <Button type="submit">Enviar</Button>
            </form>
        </div>
    );
};

// Página principal do Jogo
function GamePage() {
    const navigate = useNavigate();
    const socket = useContext(WebSocketContext);
    const [players, setPlayers] = useState<Player[]>([]);
    const [messages, setMessages] = useState<string[]>([])
    const [input, setInput] = useState("")
    const [onlineUsers, setOnlineUsers] = useState<string[]>([])
    const [dropdownOpen, setDropdownOpen] = useState(false)
    const [selectedUser, setSelectedUser] = useState<string | null>(null)
    const [sidePanelOpen, setSidePanelOpen] = useState(false)
    const [invites, setInvites] = useState<{ from: string, message: string }[]>([])
    const [inviteModalOpen, setInviteModalOpen] = useState(false)
    const [currentInvite, setCurrentInvite] = useState<{ from: string, message: string } | null>(null)
    const chatEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        if (socket && socket.current) {
            socket.current.onmessage = (event) => {
                const serverMessage = JSON.parse(event.data);
                switch (serverMessage.type) {
                    case "currentPlayers":
                        setOnlineUsers(serverMessage.payload);
                        break;

                    case "globalChat":
                        if (Array.isArray(serverMessage.payload)) {
                            setMessages(serverMessage.payload.filter(msg => typeof msg === 'string'));
                        } else if (typeof serverMessage.payload === 'string') {
                            setMessages([serverMessage.payload]);
                        }
                        break;

                    case "battleRequest":
                        setInvites(prev => [...prev, {
                            from: serverMessage.payload,
                            message: `Você foi desafiado para uma batalha por ${serverMessage.payload}`
                        }])
                        break;

                    case "battleState":
                        if (serverMessage.payload.battleStatus === "BATTLE_STARTED" || serverMessage.payload.battleStatus === "BATTLE_IN_PROGRESS") {
                            navigate('/battle');
                        }

                    case "startPvpBattle":
                        const battleId = serverMessage.payload.battleId;
                        navigate('/battle', { state: { pvpBattleId: battleId } });
                        break;
                }
            };
        }
    }, [messages, socket]);

    useEffect(() => {
        if (socket && socket.current) {
            // Define o message handler específico para a página de jogo
            socket.current.onmessage = (event) => {
                const serverMessage = JSON.parse(event.data);

                switch (serverMessage.type) {
                    case 'allPlayers':
                        setPlayers(serverMessage.payload);
                        break;
                    case 'playerJoined':
                        setPlayers(prev => [...prev.filter(p => p.id !== serverMessage.payload.id), serverMessage.payload]);
                        break;
                    case 'playerLeft':
                        setPlayers(prev => prev.filter(p => p.id !== serverMessage.payload.id));
                        break;
                    case 'playerMoved':
                        setPlayers(prev => prev.map(p => p.id === serverMessage.payload.id ? serverMessage.payload : p));
                        break;
                    case 'globalChat':
                        setMessages(serverMessage.payload);
                        break;
                }
            };

            // Solicita o estado inicial ao servidor
            socket.current.send(JSON.stringify({ type: "requestInitialState" }));
        }

        // Função de limpeza para remover o handler quando o componente desmontar
        return () => {
            if (socket && socket.current) {
                socket.current.onmessage = null;
            }
        }
    }, [socket]);

    // Gerenciador de movimento do teclado
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            const directions: { [key: string]: string } = {
                'ArrowUp': 'up', 'ArrowDown': 'down', 'ArrowLeft': 'left', 'ArrowRight': 'right'
            };
            const direction = directions[e.key];
            if (direction && socket?.current?.readyState === WebSocket.OPEN) {
                socket.current.send(JSON.stringify({ type: "move", payload: { direction } }));
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [socket]);

    const sendMessage = (e: React.FormEvent) => {
        e.preventDefault()
        if (input.trim() && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "chat",
                payload: input
            }))
            setInput("")
        }
    }

    const handleUserClick = (user: string) => {
        setSelectedUser(user)
        setSidePanelOpen(true)
        setDropdownOpen(false)
    }

    const handleChallenge = () => {
        if (selectedUser && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "requestBattle",
                payload: selectedUser
            }))
            setSidePanelOpen(false)
        }
    }

    const handleOpenInviteModal = () => {
        if (invites.length > 0) {
            setCurrentInvite(invites[0])
            setInviteModalOpen(true)
        }
    }

    const handleInviteResponse = (accept: boolean) => {
        if (currentInvite && socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({
                type: "inviteResponse",
                payload: {
                    from: currentInvite.from,
                    accept: accept
                }
            }))
        }
        setInvites(prev => prev.slice(1))
        setInviteModalOpen(false)
        setCurrentInvite(null)
    }

    useEffect(() => {
        if (socket && socket.current) {
            // Interval para atualizar jogadores e chat a cada 500ms
            const interval = setInterval(() => {
                if (socket.current && socket.current.readyState === WebSocket.OPEN) {
                    socket.current.send(JSON.stringify({ type: "retrieveCurrentPlayers" }));
                    socket.current.send(JSON.stringify({ type: "retrieveGlobalChat" }));
                }
            }, 500);

            return () => {
                clearInterval(interval);
                if (socket && socket.current) {
                    socket.current.onmessage = null;
                }
            }
        }
    }, [socket]);

    return (
        <div className="flex min-h-svh flex-col items-center justify-center relative">
            <div className="flex flex-row gap-8 w-full max-w-2xl">
                <div className="flex min-h-svh flex-row items-start justify-center gap-8 p-4 bg-gray-200">
                    <div className="flex flex-col gap-4">
                        <Map players={players} />
                        <Button onClick={() => navigate('/battle')}>Ir para Batalha Aleatória</Button>
                    </div>
                </div>
                <div className="flex flex-col flex-1 border rounded-lg p-4 bg-white shadow h-[400px]">
                    <div className="font-bold mb-2">Chat</div>
                    <div className="bg-gray-100 rounded-lg p-4 h-32 overflow-y-auto">
                        <div className="text-sm space-y-1">
                            {messages.map((message, index) => (
                                <div key={index} className="text-gray-800">{message}</div>
                            ))}
                        </div>
                    </div>
                    <form onSubmit={sendMessage} className="flex gap-2">
                        <input
                            className="flex-1 border rounded px-2 py-1"
                            value={input}
                            onChange={e => setInput(e.target.value)}
                            placeholder="Digite sua mensagem..."
                        />
                        <Button type="submit">Enviar</Button>
                    </form>
                    <div className="relative flex flex-column items-center gap-10 mt-10">
                    <Button onClick={() => setDropdownOpen(v => !v)}>
                        Usuários Online
                    </Button>
                    <button
                        className="relative ml-2 p-2 rounded-full bg-white border hover:bg-gray-100"
                        onClick={handleOpenInviteModal}
                        disabled={invites.length === 0}
                        title="Convites de batalha"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V4a2 2 0 10-4 0v1.341C7.67 7.165 6 9.388 6 12v2.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                        </svg>
                        {invites.length > 0 && (
                            <span className="absolute top-0 right-0 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white bg-red-600 rounded-full">
                                {invites.length}
                            </span>
                        )}
                    </button>
                    {dropdownOpen && (
                        <div className="absolute z-10 mt-2 w-48 bg-white border rounded shadow left-0">
                            {onlineUsers.length === 0 && (
                                <div className="px-4 py-2 text-gray-500">Nenhum usuário online</div>
                            )}
                            {onlineUsers.map(user => (
                                <button
                                    disabled={user === localStorage.getItem("nickname")}
                                    key={user}
                                    className={`w-full text-left px-4 py-2 hover:bg-gray-100 ${user === localStorage.getItem("nickname")? "bg-gray-200" : ""}`}
                                    onClick={() => handleUserClick(user)}
                                    type="button"
                                >
                                    {user}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
                </div>
            </div>
            {sidePanelOpen && selectedUser && (
                <div className="fixed top-0 right-0 h-full w-80 bg-white shadow-lg border-l flex flex-col p-6 z-20">
                    <div className="flex justify-between items-center mb-4">
                        <div className="font-bold text-lg">{selectedUser}</div>
                        <button onClick={() => setSidePanelOpen(false)} className="text-gray-500 hover:text-black">&times;</button>
                    </div>
                    <div className="flex-1 flex flex-col justify-center items-center">
                        <Button onClick={handleChallenge}>Desafiar para batalha</Button>
                    </div>
                </div>
            )}
            {inviteModalOpen && currentInvite && (
                <div className="fixed inset-0 flex items-center justify-center z-30 bg-black bg-opacity-30">
                    <div className="bg-white rounded-lg shadow-lg p-8 flex flex-col items-center">
                        <div className="mb-4 text-lg font-semibold">{currentInvite.message}</div>
                        <div className="flex gap-4">
                            <Button onClick={() => handleInviteResponse(true)}>Sim</Button>
                            <Button variant="secondary" onClick={() => handleInviteResponse(false)}>Não</Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );



}


export default GamePage;




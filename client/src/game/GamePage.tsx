import React, { useContext, useEffect, useState, useRef } from 'react';
import { WebSocketContext } from '@/WebSocketContext';
import { Button } from "@/components/ui/button";
import { useNavigate } from 'react-router-dom';

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
        background: '#a0d080',
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
    const [messages, setMessages] = useState<string[]>([]);

    useEffect(() => {
        if (socket && socket.current) {
            // Define o message handler específico para a página de jogo
            socket.current.onmessage = (event) => {
                const serverMessage = JSON.parse(event.data);
                console.log("GamePage received message: ", serverMessage); // Log para depuração
                
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
            if(socket && socket.current) {
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

    // Função para enviar mensagem de chat
    const sendMessage = (message: string) => {
        if (socket?.current?.readyState === WebSocket.OPEN) {
            socket.current.send(JSON.stringify({ type: "chat", payload: message }));
        }
    };

    return (
        <div className="flex min-h-svh flex-row items-center justify-center gap-8 p-4 bg-gray-200">
            <div className="flex flex-col gap-4">
               <Map players={players} />
               <Button onClick={() => navigate('/battle')}>Ir para Batalha Aleatória</Button>
            </div>
            <Chat messages={messages} onSendMessage={sendMessage} />
        </div>
    );
}

export default GamePage;

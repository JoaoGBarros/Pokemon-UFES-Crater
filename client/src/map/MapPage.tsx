import React, { useContext, useEffect, useState } from 'react';
import { WebSocketContext } from '@/WebSocketContext';

interface Player {
    id: number;
    nickname: string;
    posX: number;
    posY: number;
}

const TILE_SIZE = 32; // Tamanho de cada tile em pixels
const MAP_WIDTH = 20;
const MAP_HEIGHT = 40;

function MapPage() {
    const socket = useContext(WebSocketContext);
    const [players, setPlayers] = useState<Player[]>([]);

    useEffect(() => {
        if (socket && socket.current) {
             socket.current.send(JSON.stringify({ type: "requestInitialPlayers" }));

            socket.current.onmessage = (event) => {
                const serverMessage = JSON.parse(event.data);

                if (serverMessage.type === 'allPlayers') {
                    setPlayers(serverMessage.payload);
                }
                
                if (serverMessage.type === 'playerMoved') {
                    setPlayers(prevPlayers => {
                        const updatedPlayers = prevPlayers.map(p =>
                            p.id === serverMessage.payload.id ? serverMessage.payload : p
                        );
                        if (!updatedPlayers.some(p => p.id === serverMessage.payload.id)) {
                            updatedPlayers.push(serverMessage.payload);
                        }
                        return updatedPlayers;
                    });
                }

                if (serverMessage.type === 'playerJoined') {
                     setPlayers(prevPlayers => [...prevPlayers, serverMessage.payload]);
                }
            };
        }
    }, [socket]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            let direction = '';
            switch (e.key) {
                case 'ArrowUp':
                    direction = 'up';
                    break;
                case 'ArrowDown':
                    direction = 'down';
                    break;
                case 'ArrowLeft':
                    direction = 'left';
                    break;
                case 'ArrowRight':
                    direction = 'right';
                    break;
                default:
                    return;
            }

            if (socket && socket.current && socket.current.readyState === WebSocket.OPEN) {
                socket.current.send(JSON.stringify({
                    type: "move",
                    payload: { direction }
                }));
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [socket]);

    return (
        <div style={{ 
            position: 'relative', 
            width: `${MAP_WIDTH * TILE_SIZE}px`, 
            height: `${MAP_HEIGHT * TILE_SIZE}px`, 
            border: '1px solid black', 
            margin: 'auto', 
            background: '#a0d080',
            overflow: 'hidden'
        }}>
            {players.map(player => (
                <div
                    key={player.id}
                    style={{
                        position: 'absolute',
                        left: player.posX * TILE_SIZE,
                        top: player.posY * TILE_SIZE,
                        width: TILE_SIZE,
                        height: TILE_SIZE,
                        backgroundColor: 'red',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transition: 'left 0.1s linear, top 0.1s linear',
                        zIndex: 3,
                        border: '1px solid black',
                        boxSizing: 'border-box'
                    }}
                >
                    {player.nickname.charAt(0).toUpperCase()}
                </div>
            ))}
        </div>
    );
}

export default MapPage;

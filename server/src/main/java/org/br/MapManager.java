package org.br;

/**
 * Gerencia a lógica do mapa, como validação de posição e colisões.
 */
public class MapManager {
    // Dimensões do mapa em tiles (baseado em Mapa0.tmx)
    private static final int MAP_WIDTH = 20;
    private static final int MAP_HEIGHT = 40;

    /**
     * Verifica se uma determinada posição (x, y) em tiles é válida e não colide com as bordas.
     * As fronteiras foram definidas com base na camada de colisão do arquivo Mapa0.tmx.
     * @param x A coordenada X do tile.
     * @param y A coordenada Y do tile.
     * @return true se a posição for válida, false caso contrário.
     */
    public static boolean isPositionValid(int x, int y) {
        // Verifica colisão com as paredes esquerda e direita (x=0 e x=19)
        if (x < 1 || x >= MAP_WIDTH - 1) {
            return false;
        }
        // Verifica colisão com as paredes superior e inferior (y=0 e y=38)
        if (y < 1 || y >= MAP_HEIGHT - 2) {
            return false;
        }
        
        // Futuramente, outras verificações de colisão (ex: com árvores, pedras) podem ser adicionadas aqui.
        return true;
    }
}

package org.br;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Rectangle;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapManager {
    private static final int TILE_SIZE = 16;
    private static final List<Rectangle> collisionObjects = new ArrayList<>();
    private static final List<SpawnZone> spawnZones = new ArrayList<>();
    private static final Random random = new Random();

    static {
        loadMapData();
    }

    private static void loadMapData() {
        try (InputStream inputStream = MapManager.class.getResourceAsStream("/maps/Mapa0.tmx")) {
            if (inputStream == null) {
                System.err.println("ERRO CRÍTICO: Não foi possível encontrar o arquivo do mapa: /maps/Mapa0.tmx.");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            NodeList objectGroups = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroups.getLength(); i++) {
                Element group = (Element) objectGroups.item(i);
                String groupName = group.getAttribute("name");

                if ("Colisao".equals(groupName)) {
                    loadObjects(group, "collision");
                } else if ("Pokespawn".equals(groupName)) {
                    loadObjects(group, "spawn");
                }
            }
            System.out.println("Mapa carregado: " + collisionObjects.size() + " objetos de colisão e " + spawnZones.size() + " zonas de spawn.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadObjects(Element group, String type) {
        NodeList objects = group.getElementsByTagName("object");
        for (int j = 0; j < objects.getLength(); j++) {
            Element obj = (Element) objects.item(j);
            int x = (int) Float.parseFloat(obj.getAttribute("x"));
            int y = (int) Float.parseFloat(obj.getAttribute("y"));
            int width = (int) Float.parseFloat(obj.getAttribute("width"));
            int height = (int) Float.parseFloat(obj.getAttribute("height"));
            Rectangle area = new Rectangle(x, y, width, height);

            if ("collision".equals(type)) {
                collisionObjects.add(area);
            } else if ("spawn".equals(type)) {
                String zoneId = obj.getAttribute("name");
                double chance = getEncounterChanceForZone(zoneId);
                spawnZones.add(new SpawnZone(area, chance));
            }
        }
    }

    private static double getEncounterChanceForZone(String zoneId) {
        switch (zoneId) {
            //chances de spawn de pokémons selvagens
            case "0": return 0.10; 
            case "1": return 0.15; 
            case "2": return 0.20; 
            case "3": return 0.15; 
            case "4": return 0.35; 
            default: return 0.0;
        }
    }

    public static boolean isPositionValid(int tileX, int tileY) {
        Rectangle playerBounds = new Rectangle(tileX * TILE_SIZE, tileY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        for (Rectangle collisionRect : collisionObjects) {
            if (playerBounds.intersects(collisionRect)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAndTriggerEncounter(int tileX, int tileY) {
        Rectangle playerBounds = new Rectangle(tileX * TILE_SIZE, tileY * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        for (SpawnZone zone : spawnZones) {
            if (zone.getArea().intersects(playerBounds)) {// verifica se o jogador está na zona de spawn
                if (random.nextDouble() < zone.getEncounterChance()) {// roda um número aleatório para verificar se o encontro acontece
                    System.out.println("Encontro de Pokémon na zona! Chance: " + zone.getEncounterChance());
                    return true; 
                }
            }
        }
        return false; // Sem encontro.
    }
}

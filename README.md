# Phantom Client (Fabric, Minecraft 1.21.11)

Teclas padrão (mudam em Opções > Controles > PhantomClient):
- C (segurar): zoom        - H: HUD
- V: auto sprint           - B: fly (duplo espaço p/ voar)
- N: speed                 - G: fullbright
- M: step (sobe 1 bloco)   - K: MobAura (ataca mobs hostis em 4 blocos)

Fly/speed/step/aura funcionam em singleplayer (mundo com cheats) ou no seu
próprio servidor com anti-cheat desligado. Em servidores públicos dão kick/ban.

## Compilar (JDK 21)
    ./gradlew build
O .jar fica em build/libs/ (não use o -sources). Coloque em mods/ com Fabric Loader + Fabric API.

Se o build reclamar de versões: gere um projeto em https://fabricmc.net/develop
(MC 1.21.11), copie o gradle.properties de lá e mantenha a pasta src/.

## Gerar o .jar sem instalar nada (GitHub Actions)
1. Crie um repositório no GitHub e suba todo o conteúdo desta pasta.
2. Aba Actions > build > Run workflow.
3. Baixe o artefato "phantomclient-jar" (use o jar sem -sources).

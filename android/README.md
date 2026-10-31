# VanishCam para Android

Adaptação nativa do fork [perabru/vanish_webcam](https://github.com/perabru/vanish_webcam). Interface em português, câmera frontal ou traseira e processamento local com CameraX e MediaPipe. Android 8.0 ou superior, processadores ARM de 32 ou 64 bits.

## Usar

1. Instale o APK e permita o acesso à câmera.
2. Apoie o aparelho e toque em **Capturar fundo**. Saia completamente do enquadramento durante os quatro segundos.
3. Volte à imagem, escolha um efeito e use **Desaparecer**, ou ative **Gestos**.
4. Use **Reaparecer** para retornar. No Goku, também é possível abaixar a mão e repetir o gesto.
5. **Salvar foto** abre o seletor de arquivos do Android para salvar uma imagem PNG, sem os botões da interface.

| Efeito | Gesto | Resultado |
| --- | --- | --- |
| Dissolver | Indicador e médio em V | Dissolução gradual de 3,6 segundos |
| Naruto | Duas mãos próximas, dedos estendidos | Fumaça e tronco que cai |
| Partículas | Indicador e médio em V | Fragmentos coloridos levados para o lado |
| Goku | Indicador e médio juntos junto à testa por 1 segundo | Teletransporte com linhas verticais |

Mantenha as mãos inteiras visíveis. Para repetir um gesto, primeiro desfaça a pose. Todos os modos também aceitam os botões. Os sinais sonoros podem ser desativados.

Após mover o celular, trocar a câmera, girar a tela ou voltar ao app, capture novamente o fundo. O layout se adapta a retrato e paisagem.

## Escopo desta versão

- Efeitos dentro do aplicativo e fotos PNG. Não há gravação de vídeo nem saída de webcam virtual para outros aplicativos.
- O modelo separa pessoas do fundo. Durante a animação, usa-se um recorte congelado do instante do acionamento para reduzir processamento. Fumaça e partículas são desenhadas no aparelho.
- Boa iluminação e fundo estático melhoram o resultado. Sombras, cabelo e objetos podem deixar resíduos; gestos não são infalíveis.
- A prévia é limitada a 640 pixels no maior lado para controlar memória e custo de inferência. As fotos têm a resolução da prévia.
- Os modelos estão incluídos no APK. O manifesto não solicita internet, microfone ou acesso geral ao armazenamento. A câmera é desligada quando o app perde o primeiro plano.
- A primeira versão é um APK de teste assinado com chave de depuração; não é uma publicação na Play Store. Para distribuição de produção, configure uma chave de assinatura privada e estável fora do repositório.

## Compilar

Abra esta pasta no Android Studio, com JDK 17, Android SDK 35 e acesso à internet para baixar dependências. Os modelos e o sprite estão em `app/src/main/assets`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Os testes unitários verificam duração, retorno, bloqueio de reacionamento por gesto mantido, reinício da contagem, rotação/espelhamento das mãos e rejeição de coordenadas inválidas. A verificação de compilação e lint não substitui testes de câmera e desempenho em um aparelho físico.

## Código

- `MainActivity`: interface, permissões, ciclo de vida, câmera e salvamento pelo seletor do sistema.
- `VisionProcessor`: modelos de mãos, rosto e segmentação, em executor único.
- `GestureRules`: geometria dos gestos, independente do Android.
- `EffectTimeline`: transições temporais dos efeitos, independente do Android.
- `EffectRenderer`: captura do fundo e composição dos efeitos.

## Origem e referências

Preserva o projeto Python original. A descrição e o sprite do tronco vêm do repositório de origem; veja `../assets/README.md`. Modelos de mão e rosto são os já presentes no fork. A segmentação usa o modelo oficial Selfie Segmenter float16 v1:

https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/1/selfie_segmenter.tflite

Documentação oficial:

- https://ai.google.dev/edge/mediapipe/solutions/vision/hand_landmarker/android
- https://ai.google.dev/edge/mediapipe/solutions/vision/image_segmenter/android
- https://developer.android.com/media/camera/camerax

O repositório consultado não incluía arquivo LICENSE na raiz; esta adaptação não atribui uma nova licença ao código, aos modelos ou aos recursos de terceiros.

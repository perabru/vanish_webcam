# VanishCam — efeitos para sua webcam

Desapareça da imagem com um gesto. O vídeo é processado no próprio computador: o programa não grava a câmera nem envia as imagens para servidores. O vídeo exibido e enviado à câmera virtual permanece sem menus ou textos sobrepostos.

## Baixar para Windows

**[Baixar VanishCam.exe](https://github.com/perabru/vanish_webcam/releases/latest/download/VanishCam.exe)** · **[Pacote ZIP com guia em português](https://github.com/perabru/vanish_webcam/releases/latest/download/VanishCam-Windows-x64.zip)**

Windows 64 bits. O executável inclui Python, os modelos de reconhecimento, as imagens e o som padrão. Os drivers opcionais de câmera e áudio virtual são instalados separadamente.

## Começar

1. Abra `VanishCam.exe`. Se baixou o ZIP, extraia o pacote primeiro.
2. Escolha **1 — Normal Vanish** ou **2 — Naruto Vanish** com o teclado ou o mouse.
3. Saia da frente da câmera por **4 segundos** para capturar o fundo.
4. Quando o terminal informar que o fundo está pronto, volte ao enquadramento.
5. Faça o gesto do modo selecionado ou pressione **V** na janela da câmera.

| Modo | Gesto | Resultado |
| --- | --- | --- |
| Normal | Indicador e médio em V | A pessoa desaparece gradualmente |
| Naruto | Palmas juntas, dedos lado a lado, ambas as mãos visíveis | Fumaça, tronco e som ao desaparecer |

Mantenha a câmera parada depois de capturar o fundo. Se a iluminação ou o cenário mudar, pressione **Espaço** e saia do quadro por mais 4 segundos.

## Teclas

Clique na janela da câmera antes de utilizar os atalhos.

| Tecla | Ação |
| --- | --- |
| V | Ativar o efeito manualmente |
| R | Reaparecer e remover o tronco |
| Espaço | Capturar novamente o fundo |
| F | Alternar tela cheia |
| Q ou Esc | Fechar e liberar os dispositivos |

## Google Meet: imagem e som

**[Leia o guia completo em português](GUIA_GOOGLE_MEET_PTBR.md).**

Para a imagem, instale [OBS Studio](https://obsproject.com/download), deixe o OBS fechado, abra o VanishCam e selecione **OBS Virtual Camera** em **Meet → Configurações → Vídeo → Câmera**.

Para transmitir **sua voz e o efeito sonoro juntos**, o guia mostra a configuração do [Voicemeeter Banana](https://vb-audio.com/Voicemeeter/banana.htm), incluindo os dispositivos de entrada e saída e como evitar eco. A câmera virtual não encaminha o som do efeito por conta própria.

## Usar seu próprio MP3 ou WAV

Feche o programa, coloque **som.mp3** ou **som.wav** ao lado de `VanishCam.exe` e abra-o novamente. No modo Naruto, esse arquivo substitui o som incluído no pacote. Se os dois existirem, o MP3 tem prioridade. Remova ou renomeie o arquivo externo para voltar ao som padrão.

Para testar o áudio sem abrir a câmera, use `TESTAR_SOM.bat` ou:

```powershell
.\VanishCam.exe --teste-som
```

Para indicar outro caminho:

```powershell
.\VanishCam.exe --mode naruto --som "C:\Sons\efeito.wav"
```

O link solicitado para personalização foi [Naruto disappear sound effect](https://www.youtube.com/watch?v=isbyPXL9p3w). O download do áudio não ficou disponível durante a preparação desta versão; o arquivo desse vídeo não está incluído. Use o recurso de áudio externo para carregar o MP3/WAV desejado sem recompilar.

## Outras opções

Abra um terminal na pasta do executável:

```powershell
.\VanishCam.exe --mode normal
.\VanishCam.exe --mode naruto
.\VanishCam.exe --camera 1 --width 1280 --height 720 --fps 30
.\VanishCam.exe --no-virtual-camera
.\VanishCam.exe --virtual-camera --virtual-backend obs
.\VanishCam.exe --gesture-debug
.\VanishCam.exe --help
```

Se a webcam estiver ocupada, feche outros aplicativos que a utilizam. Confira as permissões de câmera do Windows para aplicativos da área de trabalho. Para uma segunda câmera, tente `--camera 1`.

## Executar pelo código-fonte

Instale Python 3.11 de 64 bits. No Windows, dê dois cliques em **INICIAR.bat**. O script prepara o ambiente na pasta do projeto. As dependências e os modelos são baixados na primeira execução; depois os arquivos locais são reutilizados.

Ou execute manualmente:

```powershell
py -3.11 -m venv .venv-local
.\.venv-local\Scripts\python.exe -m pip install -r requirements.txt
.\.venv-local\Scripts\python.exe vanish_cam.py
```

Para criar o executável no Windows, use **BUILD_WINDOWS.bat**. A compilação automatizada em GitHub Actions executa os testes, valida os modelos, decodifica o MP3 incluído e testa o executável antes de publicar o download em Releases.

## Créditos

Este fork mantém o projeto original e os créditos dos recursos. A ilustração do tronco foi gerada com ImageGen e a fumaça é desenhada em tempo real por `naruto_effect.py`; detalhes em [assets/README.md](assets/README.md).

Apoio ao autor original: [PayPal](https://paypal.me/nicovic17) ou [Revolut](https://revolut.me/nicoauriemma).

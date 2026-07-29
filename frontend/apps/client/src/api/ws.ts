/**
 * WebSocket 客户端 — IM 实时通信
 */
export type WsCallback = (data: any) => void

export interface ImPacket {
  cmd: number
  seq: number
  timestamp: number
  body: any
}

class WsClient {
  private ws: WebSocket | null = null
  private seq = 0
  private handlers = new Map<number, WsCallback>()
  private onOpenCallbacks: Array<() => void> = []
  private onCloseCallbacks: Array<() => void> = []
  private reconnectTimer = 0
  private heartbeatTimer = 0
  private url = ''
  private _closed = false

  get connected() {
    return this.ws?.readyState === WebSocket.OPEN
  }

  connect(url: string) {
    this.url = url
    this._closed = false
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('[WS] 已连接')
      this.startHeartbeat()
      // 通知所有 onOpen 回调（用于发送认证等）
      for (const cb of this.onOpenCallbacks) cb()
    }

    this.ws.onmessage = (e) => {
      try {
        const packet: ImPacket = JSON.parse(e.data)
        const handler = this.handlers.get(packet.cmd)
        if (handler) handler(packet)
      } catch (err) {
        console.error('[WS] 解析失败', err)
      }
    }

    this.ws.onclose = () => {
      console.log('[WS] 断开')
      this.stopHeartbeat()
      for (const cb of this.onCloseCallbacks) cb()
      if (!this._closed) this.reconnect()
    }

    this.ws.onerror = (e) => {
      console.error('[WS] 错误', e)
    }
  }

  /** 注册命令处理器 */
  on(cmd: number, handler: WsCallback) {
    this.handlers.set(cmd, handler)
  }

  off(cmd: number) {
    this.handlers.delete(cmd)
  }

  /** 注册连接成功回调（每次连接/重连都会触发） */
  onOpen(cb: () => void) {
    this.onOpenCallbacks.push(cb)
  }

  /** 注册断开回调 */
  onClose(cb: () => void) {
    this.onCloseCallbacks.push(cb)
  }

  /** 发送消息 */
  send(cmd: number, body: any = {}) {
    if (!this.connected) return
    this.seq++
    const packet: ImPacket = {
      cmd,
      seq: this.seq,
      timestamp: Date.now(),
      body,
    }
    this.ws!.send(JSON.stringify(packet))
  }

  close() {
    this._closed = true
    this.stopHeartbeat()
    clearTimeout(this.reconnectTimer)
    this.onOpenCallbacks.length = 0
    this.onCloseCallbacks.length = 0
    this.ws?.close()
    this.ws = null
  }

  private startHeartbeat() {
    this.heartbeatTimer = window.setInterval(() => {
      this.send(0) // CMD_HEARTBEAT
    }, 120_000)
  }

  private stopHeartbeat() {
    clearInterval(this.heartbeatTimer)
  }

  private reconnect() {
    if (this._closed) return
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = window.setTimeout(() => {
      console.log('[WS] 重连...')
      this.connect(this.url)
    }, 3000)
  }
}

export const wsClient = new WsClient()

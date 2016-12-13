import {Component, AfterViewInit, OnDestroy, ElementRef} from '@angular/core';
import * as $ from 'jquery';

@Component({
  selector: 'term',
  template: '<div></div>'
})
export class TerminalComponent implements AfterViewInit, OnDestroy {
  private term: any;
  private mode = '__default__';
  private prompt = '$ ';
  private websocket: any;

  constructor(private element: ElementRef) {
  }

  ngAfterViewInit() {
    const dom = $(this.element.nativeElement.firstChild);
    this.term = dom['terminal']((cmd, instance) => {
      if (cmd === 'clear') {
        instance.clear();
      } else if (cmd.indexOf('mode ') == 0) {
        this.mode = cmd.substring(5);
      } else if (cmd === 'show mode') {
        this.term.echo(this.mode);
      } else if (cmd === 'history') {
        this.term.history().data().forEach(h => this.term.echo(h));
      } else {
        if (!this.websocket) {
          this.connect();
        }

        this.send({command: cmd, mode: this.mode});
      }
    }, {
        greetings: 'Terminal loaded, type a command.',
        prompt: cb => cb(this.prompt)
    });
    dom.prepend('<div class="terminal-window-handler">Terminal</div>');
    this.connect();
  }

  ngOnDestroy() {
    this.term.destroy();
    this.websocket && this.websocket.close();
  }

  private send(cmd) {
    if (!!this.websocket && this.websocket.readyState != window['WebSocket'].OPEN) {
      setTimeout(() => this.send(cmd), 1500);
    } else if (!!this.websocket) {
      this.websocket.send(JSON.stringify(cmd));
    }
  }

  private connect() {
    const url = ('http:' == window.location.protocol ? 'ws://' : 'wss://') + window.location.host + window.location.pathname + 'embedded-terminal/session';
    this.websocket = new window['WebSocket'](url);
    this.websocket.onopen = event => this.term.echo('Connected to the server.');
    this.websocket.onclose = event => {
      this.websocket = undefined;
      this.term.echo('Lost server connection');
    };
    this.websocket.onmessage = event => this.onResponse(JSON.parse(event.data));
  }

  private onResponse(response) {
    if (response.type === 'username') {
      this.prompt = response.value + ' $ ';
      this.term.set_prompt(this.prompt);
    } else {
      this.term.echo(response.response || response.error || 'no output');
    }
  }
}

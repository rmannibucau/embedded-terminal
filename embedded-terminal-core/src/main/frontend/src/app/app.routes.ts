import {TerminalComponent} from './terminal/terminal.component';

export const routes = [
  {path: 'terminal', component: TerminalComponent},
  {path: '**', component: TerminalComponent}
];

import {NgModule} from '@angular/core';
import {HttpModule} from '@angular/http';
import {RouterModule} from '@angular/router';
import {BrowserModule} from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {TerminalComponent} from './terminal/terminal.component';
import {routes} from './app.routes';

import './app.style';

@NgModule({
  imports: [ // "inheritance" of modules
    BrowserModule,
    HttpModule,
    RouterModule.forRoot(routes, { useHash: true })
  ],
  declarations: [ // our components
    TerminalComponent,
    AppComponent
  ],
  providers: [ // our services

  ],
  bootstrap:    [ AppComponent ]
})
export class AppModule { }

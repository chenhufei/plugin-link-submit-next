(function(B){"use strict";/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var bt;const H=globalThis,F=H.ShadowRoot&&(H.ShadyCSS===void 0||H.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,D=Symbol(),Q=new WeakMap;let X=class{constructor(t,e,i){if(this._$cssResult$=!0,i!==D)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o;const e=this.t;if(F&&t===void 0){const i=e!==void 0&&e.length===1;i&&(t=Q.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&Q.set(e,t))}return t}toString(){return this.cssText}};const $t=o=>new X(typeof o=="string"?o:o+"",void 0,D),vt=(o,...t)=>{const e=o.length===1?o[0]:t.reduce((i,s,n)=>i+(r=>{if(r._$cssResult$===!0)return r.cssText;if(typeof r=="number")return r;throw Error("Value passed to 'css' function must be a 'css' function result: "+r+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+o[n+1],o[0]);return new X(e,o,D)},yt=(o,t)=>{if(F)o.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(const e of t){const i=document.createElement("style"),s=H.litNonce;s!==void 0&&i.setAttribute("nonce",s),i.textContent=e.cssText,o.appendChild(i)}},tt=F?o=>o:o=>o instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return $t(e)})(o):o;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const{is:_t,defineProperty:xt,getOwnPropertyDescriptor:wt,getOwnPropertyNames:At,getOwnPropertySymbols:St,getPrototypeOf:Et}=Object,m=globalThis,et=m.trustedTypes,kt=et?et.emptyScript:"",W=m.reactiveElementPolyfillSupport,C=(o,t)=>o,N={toAttribute(o,t){switch(t){case Boolean:o=o?kt:null;break;case Object:case Array:o=o==null?o:JSON.stringify(o)}return o},fromAttribute(o,t){let e=o;switch(t){case Boolean:e=o!==null;break;case Number:e=o===null?null:Number(o);break;case Object:case Array:try{e=JSON.parse(o)}catch{e=null}}return e}},V=(o,t)=>!_t(o,t),it={attribute:!0,type:String,converter:N,reflect:!1,useDefault:!1,hasChanged:V};Symbol.metadata??(Symbol.metadata=Symbol("metadata")),m.litPropertyMetadata??(m.litPropertyMetadata=new WeakMap);let A=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??(this.l=[])).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=it){if(e.state&&(e.attribute=!1),this._$Ei(),this.prototype.hasOwnProperty(t)&&((e=Object.create(e)).wrapped=!0),this.elementProperties.set(t,e),!e.noAccessor){const i=Symbol(),s=this.getPropertyDescriptor(t,i,e);s!==void 0&&xt(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){const{get:s,set:n}=wt(this.prototype,t)??{get(){return this[e]},set(r){this[e]=r}};return{get:s,set(r){const l=s==null?void 0:s.call(this);n==null||n.call(this,r),this.requestUpdate(t,l,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??it}static _$Ei(){if(this.hasOwnProperty(C("elementProperties")))return;const t=Et(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(C("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(C("properties"))){const e=this.properties,i=[...At(e),...St(e)];for(const s of i)this.createProperty(s,e[s])}const t=this[Symbol.metadata];if(t!==null){const e=litPropertyMetadata.get(t);if(e!==void 0)for(const[i,s]of e)this.elementProperties.set(i,s)}this._$Eh=new Map;for(const[e,i]of this.elementProperties){const s=this._$Eu(e,i);s!==void 0&&this._$Eh.set(s,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const s of i)e.unshift(tt(s))}else t!==void 0&&e.push(tt(t));return e}static _$Eu(t,e){const i=e.attribute;return i===!1?void 0:typeof i=="string"?i:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){var t;this._$ES=new Promise(e=>this.enableUpdating=e),this._$AL=new Map,this._$E_(),this.requestUpdate(),(t=this.constructor.l)==null||t.forEach(e=>e(this))}addController(t){var e;(this._$EO??(this._$EO=new Set)).add(t),this.renderRoot!==void 0&&this.isConnected&&((e=t.hostConnected)==null||e.call(t))}removeController(t){var e;(e=this._$EO)==null||e.delete(t)}_$E_(){const t=new Map,e=this.constructor.elementProperties;for(const i of e.keys())this.hasOwnProperty(i)&&(t.set(i,this[i]),delete this[i]);t.size>0&&(this._$Ep=t)}createRenderRoot(){const t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return yt(t,this.constructor.elementStyles),t}connectedCallback(){var t;this.renderRoot??(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),(t=this._$EO)==null||t.forEach(e=>{var i;return(i=e.hostConnected)==null?void 0:i.call(e)})}enableUpdating(t){}disconnectedCallback(){var t;(t=this._$EO)==null||t.forEach(e=>{var i;return(i=e.hostDisconnected)==null?void 0:i.call(e)})}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$ET(t,e){var n;const i=this.constructor.elementProperties.get(t),s=this.constructor._$Eu(t,i);if(s!==void 0&&i.reflect===!0){const r=(((n=i.converter)==null?void 0:n.toAttribute)!==void 0?i.converter:N).toAttribute(e,i.type);this._$Em=t,r==null?this.removeAttribute(s):this.setAttribute(s,r),this._$Em=null}}_$AK(t,e){var n,r;const i=this.constructor,s=i._$Eh.get(t);if(s!==void 0&&this._$Em!==s){const l=i.getPropertyOptions(s),a=typeof l.converter=="function"?{fromAttribute:l.converter}:((n=l.converter)==null?void 0:n.fromAttribute)!==void 0?l.converter:N;this._$Em=s;const c=a.fromAttribute(e,l.type);this[s]=c??((r=this._$Ej)==null?void 0:r.get(s))??c,this._$Em=null}}requestUpdate(t,e,i,s=!1,n){var r;if(t!==void 0){const l=this.constructor;if(s===!1&&(n=this[t]),i??(i=l.getPropertyOptions(t)),!((i.hasChanged??V)(n,e)||i.useDefault&&i.reflect&&n===((r=this._$Ej)==null?void 0:r.get(t))&&!this.hasAttribute(l._$Eu(t,i))))return;this.C(t,e,i)}this.isUpdatePending===!1&&(this._$ES=this._$EP())}C(t,e,{useDefault:i,reflect:s,wrapped:n},r){i&&!(this._$Ej??(this._$Ej=new Map)).has(t)&&(this._$Ej.set(t,r??e??this[t]),n!==!0||r!==void 0)||(this._$AL.has(t)||(this.hasUpdated||i||(e=void 0),this._$AL.set(t,e)),s===!0&&this._$Em!==t&&(this._$Eq??(this._$Eq=new Set)).add(t))}async _$EP(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}const t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var i;if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??(this.renderRoot=this.createRenderRoot()),this._$Ep){for(const[n,r]of this._$Ep)this[n]=r;this._$Ep=void 0}const s=this.constructor.elementProperties;if(s.size>0)for(const[n,r]of s){const{wrapped:l}=r,a=this[n];l!==!0||this._$AL.has(n)||a===void 0||this.C(n,void 0,r,a)}}let t=!1;const e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),(i=this._$EO)==null||i.forEach(s=>{var n;return(n=s.hostUpdate)==null?void 0:n.call(s)}),this.update(e)):this._$EM()}catch(s){throw t=!1,this._$EM(),s}t&&this._$AE(e)}willUpdate(t){}_$AE(t){var e;(e=this._$EO)==null||e.forEach(i=>{var s;return(s=i.hostUpdated)==null?void 0:s.call(i)}),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EM(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Eq&&(this._$Eq=this._$Eq.forEach(e=>this._$ET(e,this[e]))),this._$EM()}updated(t){}firstUpdated(t){}};A.elementStyles=[],A.shadowRootOptions={mode:"open"},A[C("elementProperties")]=new Map,A[C("finalized")]=new Map,W==null||W({ReactiveElement:A}),(m.reactiveElementVersions??(m.reactiveElementVersions=[])).push("2.1.2");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const P=globalThis,st=o=>o,q=P.trustedTypes,rt=q?q.createPolicy("lit-html",{createHTML:o=>o}):void 0,ot="$lit$",g=`lit$${Math.random().toFixed(9).slice(2)}$`,nt="?"+g,Ct=`<${nt}>`,v=document,T=()=>v.createComment(""),O=o=>o===null||typeof o!="object"&&typeof o!="function",J=Array.isArray,Pt=o=>J(o)||typeof(o==null?void 0:o[Symbol.iterator])=="function",K=`[ 	
\f\r]`,R=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,at=/-->/g,lt=/>/g,y=RegExp(`>|${K}(?:([^\\s"'>=/]+)(${K}*=${K}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),ct=/'/g,ht=/"/g,dt=/^(?:script|style|textarea|title)$/i,Tt=o=>(t,...e)=>({_$litType$:o,strings:t,values:e}),S=Tt(1),E=Symbol.for("lit-noChange"),d=Symbol.for("lit-nothing"),pt=new WeakMap,_=v.createTreeWalker(v,129);function ut(o,t){if(!J(o)||!o.hasOwnProperty("raw"))throw Error("invalid template strings array");return rt!==void 0?rt.createHTML(t):t}const Ot=(o,t)=>{const e=o.length-1,i=[];let s,n=t===2?"<svg>":t===3?"<math>":"",r=R;for(let l=0;l<e;l++){const a=o[l];let c,p,h=-1,f=0;for(;f<a.length&&(r.lastIndex=f,p=r.exec(a),p!==null);)f=r.lastIndex,r===R?p[1]==="!--"?r=at:p[1]!==void 0?r=lt:p[2]!==void 0?(dt.test(p[2])&&(s=RegExp("</"+p[2],"g")),r=y):p[3]!==void 0&&(r=y):r===y?p[0]===">"?(r=s??R,h=-1):p[1]===void 0?h=-2:(h=r.lastIndex-p[2].length,c=p[1],r=p[3]===void 0?y:p[3]==='"'?ht:ct):r===ht||r===ct?r=y:r===at||r===lt?r=R:(r=y,s=void 0);const $=r===y&&o[l+1].startsWith("/>")?" ":"";n+=r===R?a+Ct:h>=0?(i.push(c),a.slice(0,h)+ot+a.slice(h)+g+$):a+g+(h===-2?l:$)}return[ut(o,n+(o[e]||"<?>")+(t===2?"</svg>":t===3?"</math>":"")),i]};class U{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let n=0,r=0;const l=t.length-1,a=this.parts,[c,p]=Ot(t,e);if(this.el=U.createElement(c,i),_.currentNode=this.el.content,e===2||e===3){const h=this.el.content.firstChild;h.replaceWith(...h.childNodes)}for(;(s=_.nextNode())!==null&&a.length<l;){if(s.nodeType===1){if(s.hasAttributes())for(const h of s.getAttributeNames())if(h.endsWith(ot)){const f=p[r++],$=s.getAttribute(h).split(g),z=/([.?@])?(.*)/.exec(f);a.push({type:1,index:n,name:z[2],strings:$,ctor:z[1]==="."?Ut:z[1]==="?"?Mt:z[1]==="@"?It:j}),s.removeAttribute(h)}else h.startsWith(g)&&(a.push({type:6,index:n}),s.removeAttribute(h));if(dt.test(s.tagName)){const h=s.textContent.split(g),f=h.length-1;if(f>0){s.textContent=q?q.emptyScript:"";for(let $=0;$<f;$++)s.append(h[$],T()),_.nextNode(),a.push({type:2,index:++n});s.append(h[f],T())}}}else if(s.nodeType===8)if(s.data===nt)a.push({type:2,index:n});else{let h=-1;for(;(h=s.data.indexOf(g,h+1))!==-1;)a.push({type:7,index:n}),h+=g.length-1}n++}}static createElement(t,e){const i=v.createElement("template");return i.innerHTML=t,i}}function k(o,t,e=o,i){var r,l;if(t===E)return t;let s=i!==void 0?(r=e._$Co)==null?void 0:r[i]:e._$Cl;const n=O(t)?void 0:t._$litDirective$;return(s==null?void 0:s.constructor)!==n&&((l=s==null?void 0:s._$AO)==null||l.call(s,!1),n===void 0?s=void 0:(s=new n(o),s._$AT(o,e,i)),i!==void 0?(e._$Co??(e._$Co=[]))[i]=s:e._$Cl=s),s!==void 0&&(t=k(o,s._$AS(o,t.values),s,i)),t}class Rt{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){const{el:{content:e},parts:i}=this._$AD,s=((t==null?void 0:t.creationScope)??v).importNode(e,!0);_.currentNode=s;let n=_.nextNode(),r=0,l=0,a=i[0];for(;a!==void 0;){if(r===a.index){let c;a.type===2?c=new M(n,n.nextSibling,this,t):a.type===1?c=new a.ctor(n,a.name,a.strings,this,t):a.type===6&&(c=new Ht(n,this,t)),this._$AV.push(c),a=i[++l]}r!==(a==null?void 0:a.index)&&(n=_.nextNode(),r++)}return _.currentNode=v,s}p(t){let e=0;for(const i of this._$AV)i!==void 0&&(i.strings!==void 0?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class M{get _$AU(){var t;return((t=this._$AM)==null?void 0:t._$AU)??this._$Cv}constructor(t,e,i,s){this.type=2,this._$AH=d,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cv=(s==null?void 0:s.isConnected)??!0}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return e!==void 0&&(t==null?void 0:t.nodeType)===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=k(this,t,e),O(t)?t===d||t==null||t===""?(this._$AH!==d&&this._$AR(),this._$AH=d):t!==this._$AH&&t!==E&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):Pt(t)?this.k(t):this._(t)}O(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}_(t){this._$AH!==d&&O(this._$AH)?this._$AA.nextSibling.data=t:this.T(v.createTextNode(t)),this._$AH=t}$(t){var n;const{values:e,_$litType$:i}=t,s=typeof i=="number"?this._$AC(t):(i.el===void 0&&(i.el=U.createElement(ut(i.h,i.h[0]),this.options)),i);if(((n=this._$AH)==null?void 0:n._$AD)===s)this._$AH.p(e);else{const r=new Rt(s,this),l=r.u(this.options);r.p(e),this.T(l),this._$AH=r}}_$AC(t){let e=pt.get(t.strings);return e===void 0&&pt.set(t.strings,e=new U(t)),e}k(t){J(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const n of t)s===e.length?e.push(i=new M(this.O(T()),this.O(T()),this,this.options)):i=e[s],i._$AI(n),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){var i;for((i=this._$AP)==null?void 0:i.call(this,!1,!0,e);t!==this._$AB;){const s=st(t).nextSibling;st(t).remove(),t=s}}setConnected(t){var e;this._$AM===void 0&&(this._$Cv=t,(e=this._$AP)==null||e.call(this,t))}}class j{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,i,s,n){this.type=1,this._$AH=d,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=n,i.length>2||i[0]!==""||i[1]!==""?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=d}_$AI(t,e=this,i,s){const n=this.strings;let r=!1;if(n===void 0)t=k(this,t,e,0),r=!O(t)||t!==this._$AH&&t!==E,r&&(this._$AH=t);else{const l=t;let a,c;for(t=n[0],a=0;a<n.length-1;a++)c=k(this,l[i+a],e,a),c===E&&(c=this._$AH[a]),r||(r=!O(c)||c!==this._$AH[a]),c===d?t=d:t!==d&&(t+=(c??"")+n[a+1]),this._$AH[a]=c}r&&!s&&this.j(t)}j(t){t===d?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}}class Ut extends j{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===d?void 0:t}}class Mt extends j{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==d)}}class It extends j{constructor(t,e,i,s,n){super(t,e,i,s,n),this.type=5}_$AI(t,e=this){if((t=k(this,t,e,0)??d)===E)return;const i=this._$AH,s=t===d&&i!==d||t.capture!==i.capture||t.once!==i.once||t.passive!==i.passive,n=t!==d&&(i===d||s);s&&this.element.removeEventListener(this.name,this,i),n&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){var e;typeof this._$AH=="function"?this._$AH.call(((e=this.options)==null?void 0:e.host)??this.element,t):this._$AH.handleEvent(t)}}class Ht{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){k(this,t)}}const Z=P.litHtmlPolyfillSupport;Z==null||Z(U,M),(P.litHtmlVersions??(P.litHtmlVersions=[])).push("3.3.3");const Nt=(o,t,e)=>{const i=(e==null?void 0:e.renderBefore)??t;let s=i._$litPart$;if(s===void 0){const n=(e==null?void 0:e.renderBefore)??null;i._$litPart$=s=new M(t.insertBefore(T(),n),n,void 0,e??{})}return s._$AI(o),s};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const x=globalThis;class I extends A{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){var e;const t=super.createRenderRoot();return(e=this.renderOptions).renderBefore??(e.renderBefore=t.firstChild),t}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=Nt(e,this.renderRoot,this.renderOptions)}connectedCallback(){var t;super.connectedCallback(),(t=this._$Do)==null||t.setConnected(!0)}disconnectedCallback(){var t;super.disconnectedCallback(),(t=this._$Do)==null||t.setConnected(!1)}render(){return E}}I._$litElement$=!0,I.finalized=!0,(bt=x.litElementHydrateSupport)==null||bt.call(x,{LitElement:I});const Y=x.litElementPolyfillSupport;Y==null||Y({LitElement:I}),(x.litElementVersions??(x.litElementVersions=[])).push("4.2.2");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const qt={attribute:!0,type:String,converter:N,reflect:!1,hasChanged:V},jt=(o=qt,t,e)=>{const{kind:i,metadata:s}=e;let n=globalThis.litPropertyMetadata.get(s);if(n===void 0&&globalThis.litPropertyMetadata.set(s,n=new Map),i==="setter"&&((o=Object.create(o)).wrapped=!0),n.set(e.name,o),i==="accessor"){const{name:r}=e;return{set(l){const a=t.get.call(this);t.set.call(this,l),this.requestUpdate(r,a,o,!0,l)},init(l){return l!==void 0&&this.C(r,void 0,o,l),l}}}if(i==="setter"){const{name:r}=e;return function(l){const a=this[r];t.call(this,l),this.requestUpdate(r,a,o,!0,l)}}throw Error("Unsupported decorator location: "+i)};function ft(o){return(t,e)=>typeof e=="object"?jt(o,t,e):((i,s,n)=>{const r=s.hasOwnProperty(n);return s.constructor.createProperty(n,i),r?Object.getOwnPropertyDescriptor(s,n):void 0})(o,t,e)}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function w(o){return ft({...o,state:!0,attribute:!1})}var Lt=Object.defineProperty,b=(o,t,e,i)=>{for(var s=void 0,n=o.length-1,r;n>=0;n--)(r=o[n])&&(s=r(t,e,s)||s);return s&&Lt(t,e,s),s};const mt="/apis/api.link.halo.run/v1alpha1/link-applications",zt=`${mt}/captcha`;function Bt(o,t){const e=String(o.get("rssUrl")||"").trim();return{url:String(o.get("url")||"").trim(),displayName:String(o.get("displayName")||"").trim(),logo:String(o.get("logo")||"").trim()||null,description:String(o.get("description")||"").trim()||null,email:String(o.get("email")||"").trim()||null,backlink:String(o.get("backlink")||"").trim()||null,feedUrls:e?[e]:[],challengeId:t,captchaCode:String(o.get("captchaCode")||"").trim()}}async function L(o){const t=await o.text();if(!t)return null;try{return JSON.parse(t)}catch{throw new Error("接口返回格式错误")}}const G=class G extends I{constructor(){super(),this.open=!1,this.submitting=!1,this.fetchingSite=!1,this.sitePreviewEnabled=!0,this.captchaLoading=!1,this.toastMessage="",this.toastType="success",this.siteInfoRequest=0,this.captchaRequest=0,this.previousBodyOverflow="",this.lastFetchedSiteInfo={title:"",logo:"",description:""},this.fetchConfiguration()}willUpdate(t){t.has("open")&&(this.open?(this.previousBodyOverflow=document.body.style.overflow,document.body.style.overflow="hidden",this.captcha||this.fetchCaptcha()):document.body.style.overflow=this.previousBodyOverflow)}updated(t){t.has("open")&&this.open&&requestAnimationFrame(()=>{var e,i;return(i=(e=this.shadowRoot)==null?void 0:e.querySelector("#input-url"))==null?void 0:i.focus()})}disconnectedCallback(){this.siteInfoRequest+=1,this.captchaRequest+=1,window.clearTimeout(this.toastTimer),window.clearTimeout(this.captchaExpiryTimer),document.body.style.overflow=this.previousBodyOverflow,super.disconnectedCallback()}showToast(t,e="success"){window.clearTimeout(this.toastTimer),this.toastMessage=t,this.toastType=e,this.toastTimer=window.setTimeout(()=>{this.toastMessage=""},3600)}async fetchConfiguration(){try{const t=await fetch("/apis/api.link.submit.halo.run/v1alpha1/configuration",{credentials:"same-origin",headers:{Accept:"application/json"}});if(!t.ok)return;const e=await L(t);this.sitePreviewEnabled=(e==null?void 0:e.linkPreviewEnabled)!==!1}catch(t){console.error("Failed to load link enhancement configuration:",t)}}async fetchCaptcha(){const t=++this.captchaRequest;this.captchaLoading=!0,window.clearTimeout(this.captchaExpiryTimer);try{const e=await fetch(zt,{method:"POST",credentials:"same-origin",headers:{Accept:"application/json"},cache:"no-store"}),i=await L(e);if(t!==this.captchaRequest)return;if(!e.ok||!(i!=null&&i.challengeId)||!i.image)throw new Error((i==null?void 0:i.detail)||(i==null?void 0:i.title)||"验证码加载失败");this.captcha=i;const s=Math.max(15,i.expiresInSeconds-10)*1e3;this.captchaExpiryTimer=window.setTimeout(()=>{this.captcha=void 0,this.open&&this.fetchCaptcha()},s)}catch(e){t===this.captchaRequest&&(this.captcha=void 0,this.showToast(e instanceof Error?e.message:"验证码加载失败","error"))}finally{t===this.captchaRequest&&(this.captchaLoading=!1)}}async fetchSiteInfo(){var s;const t=(s=this.shadowRoot)==null?void 0:s.querySelector("#input-url");if(!(t!=null&&t.value.trim())){this.showToast("请先填写网址","error");return}let e=t.value.trim();/^https?:\/\//i.test(e)||(e=`https://${e}`,t.value=e);const i=++this.siteInfoRequest;this.fetchingSite=!0;try{const n=await fetch(`/apis/api.link.submit.halo.run/v1alpha1/site-info?url=${encodeURIComponent(e)}`,{credentials:"same-origin",headers:{Accept:"application/json"},cache:"no-store"}),r=await L(n);if(i!==this.siteInfoRequest)return;if(!n.ok){this.showToast((r==null?void 0:r.detail)||"获取网站信息失败，请手动填写","error");return}this.fillSiteInfo(r==null?void 0:r.title,r==null?void 0:r.logo,r==null?void 0:r.description),this.showToast(r!=null&&r.title||r!=null&&r.description||r!=null&&r.logo?"已自动填充网站信息":"未获取到网站信息，请手动填写",r!=null&&r.title||r!=null&&r.description||r!=null&&r.logo?"success":"error")}catch(n){i===this.siteInfoRequest&&this.showToast(n instanceof Error?n.message:"获取网站信息失败","error")}finally{i===this.siteInfoRequest&&(this.fetchingSite=!1)}}fillSiteInfo(t,e,i){var l,a,c;const s=(l=this.shadowRoot)==null?void 0:l.querySelector("#input-name"),n=(a=this.shadowRoot)==null?void 0:a.querySelector("#input-logo"),r=(c=this.shadowRoot)==null?void 0:c.querySelector("#textarea-description");s&&this.replaceFetchedValue(s,t,this.lastFetchedSiteInfo.title),n&&this.replaceFetchedValue(n,e,this.lastFetchedSiteInfo.logo),r&&this.replaceFetchedValue(r,i,this.lastFetchedSiteInfo.description),this.lastFetchedSiteInfo={title:(t==null?void 0:t.trim())||"",logo:(e==null?void 0:e.trim())||"",description:(i==null?void 0:i.trim())||""}}replaceFetchedValue(t,e,i){const s=t.value.trim();(!s||s===i)&&(t.value=(e==null?void 0:e.trim())||"")}handleClose(){this.siteInfoRequest+=1,this.fetchingSite=!1,this.open=!1}handleKeydown(t){t.key==="Escape"&&(t.preventDefault(),this.handleClose())}async handleSubmit(t){var i;if(t.preventDefault(),!((i=this.captcha)!=null&&i.challengeId)){this.showToast("验证码尚未加载，请刷新后重试","error");return}this.submitting=!0;const e=t.currentTarget;try{const s=await fetch(mt,{method:"POST",credentials:"same-origin",headers:{"Content-Type":"application/json",Accept:"application/json, application/problem+json"},body:JSON.stringify(Bt(new FormData(e),this.captcha.challengeId))}),n=await L(s);if(!s.ok){this.showToast((n==null?void 0:n.detail)||(n==null?void 0:n.title)||"提交失败，请检查表单","error"),await this.fetchCaptcha();return}this.showToast("申请已提交到官方友链审核，请等待处理"),e.reset(),this.captcha=void 0,window.setTimeout(()=>this.handleClose(),1200)}catch(s){this.showToast(s instanceof Error?s.message:"提交失败，请稍后重试","error"),await this.fetchCaptcha()}finally{this.submitting=!1}}renderForm(){var t;return S`
      <header class="modal-header">
        <div>
          <h2 id="link-submit-modal-title">申请友链</h2>
          <p>申请将进入 Halo 官方链接插件审核，本插件仅提供表单与信息提取增强。</p>
        </div>
        <button
          type="button"
          class="icon-button"
          aria-label="关闭申请窗口"
          @click=${this.handleClose}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6 6 18" /></svg>
        </button>
      </header>
      <form @submit=${this.handleSubmit}>
        <div class="field field-wide">
          <label for="input-url">网站地址 <span>*</span></label>
          <div class="url-row">
            <input
              id="input-url"
              name="url"
              type="url"
              placeholder="https://example.com"
              required
            />
            ${this.sitePreviewEnabled?S`<button
                    type="button"
                    class="secondary-button"
                    ?disabled=${this.fetchingSite}
                    @click=${this.fetchSiteInfo}
                  >
                    ${this.fetchingSite?"获取中...":"获取信息"}
                  </button>`:""}
          </div>
          <small>修改网址后可再次获取，已自动填入的标题、Logo 和描述会同步更新。</small>
        </div>

        <div class="field-grid">
          <div class="field">
            <label for="input-name">网站名称 <span>*</span></label>
            <input id="input-name" name="displayName" type="text" required />
          </div>
          <div class="field">
            <label for="input-email">联系邮箱</label>
            <input id="input-email" name="email" type="email" autocomplete="email" />
          </div>
          <div class="field">
            <label for="input-logo">Logo 地址</label>
            <input id="input-logo" name="logo" type="url" placeholder="https://..." />
          </div>
          <div class="field">
            <label for="input-rss">RSS / Atom</label>
            <input id="input-rss" name="rssUrl" type="url" placeholder="https://.../feed.xml" />
          </div>
        </div>

        <div class="field">
          <label for="input-backlink">本站友链页面</label>
          <input
            id="input-backlink"
            name="backlink"
            type="url"
            placeholder="已添加本站链接的页面地址"
          />
        </div>

        <div class="field">
          <label for="textarea-description">网站描述</label>
          <textarea id="textarea-description" name="description" rows="3"></textarea>
        </div>

        <div class="captcha-row">
          <div class="field captcha-input">
            <label for="input-captcha">验证码 <span>*</span></label>
            <input id="input-captcha" name="captchaCode" type="text" autocomplete="off" required />
          </div>
          <button
            type="button"
            class="captcha-image"
            aria-label="刷新验证码"
            ?disabled=${this.captchaLoading}
            @click=${this.fetchCaptcha}
          >
            ${(t=this.captcha)!=null&&t.image?S`<img src=${this.captcha.image} alt="友链申请验证码" />`:S`<span>${this.captchaLoading?"加载中":"点击刷新"}</span>`}
          </button>
        </div>

        <footer class="modal-footer">
          <button type="button" class="secondary-button" @click=${this.handleClose}>取消</button>
          <button
            type="submit"
            class="primary-button"
            ?disabled=${this.submitting||this.captchaLoading||!this.captcha}
          >
            ${this.submitting?"提交中...":"提交申请"}
          </button>
        </footer>
      </form>
    `}render(){const t=this.open?"false":"true";return S`
      <div
        class="modal-wrapper ${this.open?"is-open":""}"
        aria-hidden=${t}
        @keydown=${this.handleKeydown}
      >
        <button
          class="modal-layer"
          type="button"
          aria-label="关闭申请窗口"
          @click=${this.handleClose}
        ></button>
        <section
          class="modal-content"
          role="dialog"
          aria-modal="true"
          aria-labelledby="link-submit-modal-title"
        >
          ${this.open?this.renderForm():""}
        </section>
        ${this.toastMessage?S`<div
                class="toast ${this.toastType}"
                role=${this.toastType==="error"?"alert":"status"}
                aria-live="polite"
              >
                <strong>${this.toastType==="error"?"提交提示":"操作成功"}</strong>
                <span>${this.toastMessage}</span>
              </div>`:""}
      </div>
    `}};G.styles=vt`
    :host {
      --accent: var(--link-submit-widget-form-button-bg-color, #d13e43);
      --accent-hover: var(--link-submit-widget-form-button-hover-bg-color, #b92f35);
      --surface: var(--link-submit-widget-base-bg-color, #ffffff);
      --surface-muted: color-mix(in srgb, var(--surface) 94%, #64748b);
      --text: var(--link-submit-widget-form-text-color, #18202b);
      --text-muted: var(--link-submit-widget-form-label-color, #5d6878);
      --border: var(--link-submit-widget-form-border-color, #d9dee7);
      --radius: var(--link-submit-widget-base-rounded, 8px);
      font:
        400 16px/1.55 ui-sans-serif,
        system-ui,
        -apple-system,
        BlinkMacSystemFont,
        'Segoe UI',
        sans-serif;
      color: var(--text);
    }
    * {
      box-sizing: border-box;
    }
    button,
    input,
    textarea {
      font: inherit;
      letter-spacing: 0;
    }
    .modal-wrapper {
      position: fixed;
      inset: 0;
      z-index: 999;
      display: grid;
      place-items: center;
      padding: 24px;
      visibility: hidden;
      pointer-events: none;
    }
    .modal-wrapper.is-open {
      visibility: visible;
      pointer-events: auto;
    }
    .modal-layer {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      border: 0;
      background: var(--link-submit-widget-modal-layer-color, rgb(15 23 42 / 0.64));
      opacity: 0;
      transition: opacity 180ms ease;
    }
    .is-open .modal-layer {
      opacity: 1;
    }
    .modal-content {
      position: relative;
      width: min(680px, 100%);
      max-height: min(820px, calc(100vh - 48px));
      overflow: auto;
      overscroll-behavior: contain;
      scrollbar-gutter: stable;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      box-shadow: 0 24px 70px rgb(15 23 42 / 0.24);
      opacity: 0;
      translate: 0 12px;
      transition:
        opacity 180ms ease,
        translate 220ms cubic-bezier(0.22, 1, 0.36, 1);
    }
    .is-open .modal-content {
      opacity: 1;
      translate: 0 0;
    }
    .modal-header {
      position: sticky;
      top: 0;
      z-index: 2;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 20px;
      padding: 24px 28px 18px;
      background: color-mix(in srgb, var(--surface) 94%, transparent);
      border-bottom: 1px solid var(--border);
      backdrop-filter: blur(10px);
    }
    h2 {
      margin: 0;
      font-size: 1.25rem;
      line-height: 1.3;
    }
    .modal-header p {
      margin: 6px 0 0;
      max-width: 520px;
      color: var(--text-muted);
      font-size: 0.86rem;
    }
    .icon-button {
      flex: 0 0 40px;
      width: 40px;
      height: 40px;
      display: grid;
      place-items: center;
      padding: 0;
      border: 1px solid transparent;
      border-radius: 50%;
      background: transparent;
      color: var(--text-muted);
      cursor: pointer;
    }
    .icon-button:hover {
      background: var(--surface-muted);
      color: var(--text);
    }
    .icon-button svg {
      width: 21px;
      height: 21px;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.8;
      stroke-linecap: round;
    }
    form {
      display: grid;
      gap: 20px;
      padding: 24px 28px 28px;
    }
    .field-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px;
    }
    .field {
      display: grid;
      gap: 7px;
      min-width: 0;
    }
    label {
      color: var(--text);
      font-size: 0.9rem;
      font-weight: 600;
    }
    label span {
      color: var(--accent);
    }
    small {
      color: var(--text-muted);
      font-size: 0.78rem;
    }
    input,
    textarea {
      width: 100%;
      min-width: 0;
      border: 1px solid var(--border);
      border-radius: calc(var(--radius) - 2px);
      background: var(--surface);
      color: var(--text);
      outline: none;
      transition:
        border-color 150ms ease,
        box-shadow 150ms ease;
    }
    input {
      min-height: 44px;
      padding: 0 12px;
    }
    textarea {
      resize: vertical;
      min-height: 88px;
      padding: 10px 12px;
    }
    input:focus,
    textarea:focus {
      border-color: var(--accent);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 18%, transparent);
    }
    .url-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 10px;
    }
    .captcha-row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 180px;
      gap: 14px;
      align-items: end;
    }
    .captcha-image {
      min-height: 72px;
      display: grid;
      place-items: center;
      overflow: hidden;
      padding: 4px;
      border: 1px solid var(--border);
      border-radius: calc(var(--radius) - 2px);
      background: var(--surface-muted);
      color: var(--text-muted);
      cursor: pointer;
    }
    .captcha-image img {
      display: block;
      width: 100%;
      height: 62px;
      object-fit: contain;
    }
    .primary-button,
    .secondary-button {
      min-height: 42px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 18px;
      border-radius: calc(var(--radius) - 2px);
      font-weight: 600;
      cursor: pointer;
      transition:
        background-color 150ms ease,
        border-color 150ms ease,
        color 150ms ease,
        transform 100ms ease;
    }
    .primary-button {
      border: 1px solid var(--accent);
      background: var(--accent);
      color: #fff;
    }
    .primary-button:hover:not(:disabled) {
      background: var(--accent-hover);
      border-color: var(--accent-hover);
    }
    .secondary-button {
      border: 1px solid var(--border);
      background: var(--surface);
      color: var(--text);
    }
    .secondary-button:hover:not(:disabled) {
      border-color: color-mix(in srgb, var(--accent) 45%, var(--border));
      color: var(--accent);
    }
    button:active:not(:disabled) {
      transform: translateY(1px);
    }
    button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding-top: 4px;
    }
    .toast {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 3;
      width: min(360px, calc(100vw - 40px));
      display: grid;
      gap: 3px;
      padding: 14px 16px;
      border: 1px solid #a7d7bd;
      border-radius: var(--radius);
      background: #effaf4;
      color: #14532d;
      box-shadow: 0 14px 36px rgb(15 23 42 / 0.18);
    }
    .toast.error {
      border-color: #efb0b0;
      background: #fff1f1;
      color: #8a1c1c;
    }
    .toast span {
      font-size: 0.84rem;
    }
    @media (max-width: 620px) {
      .modal-wrapper {
        align-items: end;
        padding: 0;
      }
      .modal-content {
        width: 100%;
        max-height: calc(100dvh - 12px);
        border-radius: var(--radius) var(--radius) 0 0;
      }
      .modal-header {
        padding: 20px 18px 15px;
      }
      form {
        padding: 20px 18px 24px;
      }
      .field-grid,
      .captcha-row,
      .url-row {
        grid-template-columns: minmax(0, 1fr);
      }
      .captcha-image {
        min-height: 68px;
      }
      .modal-footer {
        position: sticky;
        bottom: 0;
        margin: 0 -18px -24px;
        padding: 14px 18px calc(14px + env(safe-area-inset-bottom));
        background: var(--surface);
        border-top: 1px solid var(--border);
      }
      .modal-footer button {
        flex: 1;
      }
    }
    @media (prefers-reduced-motion: reduce) {
      *,
      *::before,
      *::after {
        scroll-behavior: auto !important;
        transition-duration: 0.01ms !important;
        animation-duration: 0.01ms !important;
      }
    }
  `;let u=G;b([ft({type:Boolean,reflect:!0})],u.prototype,"open"),b([w()],u.prototype,"submitting"),b([w()],u.prototype,"fetchingSite"),b([w()],u.prototype,"sitePreviewEnabled"),b([w()],u.prototype,"captchaLoading"),b([w()],u.prototype,"captcha"),b([w()],u.prototype,"toastMessage"),b([w()],u.prototype,"toastType"),customElements.get("link-submit-modal")||customElements.define("link-submit-modal",u);const gt=document.createElement("link-submit-modal");document.body.append(gt);function Ft(){gt.open=!0}B.LinkSubmitModal=u,B.open=Ft,Object.defineProperty(B,Symbol.toStringTag,{value:"Module"})})(this.LinkSubmitWidget=this.LinkSubmitWidget||{});

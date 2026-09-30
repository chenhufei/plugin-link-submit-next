(function(e){Object.defineProperty(e,Symbol.toStringTag,{value:`Module`});var t=globalThis,n=t.ShadowRoot&&(t.ShadyCSS===void 0||t.ShadyCSS.nativeShadow)&&`adoptedStyleSheets`in Document.prototype&&`replace`in CSSStyleSheet.prototype,r=Symbol(),i=new WeakMap,a=class{constructor(e,t,n){if(this._$cssResult$=!0,n!==r)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=e,this.t=t}get styleSheet(){let e=this.o,t=this.t;if(n&&e===void 0){let n=t!==void 0&&t.length===1;n&&(e=i.get(t)),e===void 0&&((this.o=e=new CSSStyleSheet).replaceSync(this.cssText),n&&i.set(t,e))}return e}toString(){return this.cssText}},o=e=>new a(typeof e==`string`?e:e+``,void 0,r),s=(e,...t)=>new a(e.length===1?e[0]:t.reduce((t,n,r)=>t+(e=>{if(!0===e._$cssResult$)return e.cssText;if(typeof e==`number`)return e;throw Error(`Value passed to 'css' function must be a 'css' function result: `+e+`. Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.`)})(n)+e[r+1],e[0]),e,r),c=(e,r)=>{if(n)e.adoptedStyleSheets=r.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let n of r){let r=document.createElement(`style`),i=t.litNonce;i!==void 0&&r.setAttribute(`nonce`,i),r.textContent=n.cssText,e.appendChild(r)}},l=n?e=>e:e=>e instanceof CSSStyleSheet?(e=>{let t=``;for(let n of e.cssRules)t+=n.cssText;return o(t)})(e):e,{is:u,defineProperty:d,getOwnPropertyDescriptor:ee,getOwnPropertyNames:te,getOwnPropertySymbols:ne,getPrototypeOf:re}=Object,f=globalThis,ie=f.trustedTypes,ae=ie?ie.emptyScript:``,oe=f.reactiveElementPolyfillSupport,p=(e,t)=>e,m={toAttribute(e,t){switch(t){case Boolean:e=e?ae:null;break;case Object:case Array:e=e==null?e:JSON.stringify(e)}return e},fromAttribute(e,t){let n=e;switch(t){case Boolean:n=e!==null;break;case Number:n=e===null?null:Number(e);break;case Object:case Array:try{n=JSON.parse(e)}catch{n=null}}return n}},h=(e,t)=>!u(e,t),g={attribute:!0,type:String,converter:m,reflect:!1,useDefault:!1,hasChanged:h};Symbol.metadata??=Symbol(`metadata`),f.litPropertyMetadata??=new WeakMap;var _=class extends HTMLElement{static addInitializer(e){this._$Ei(),(this.l??=[]).push(e)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(e,t=g){if(t.state&&(t.attribute=!1),this._$Ei(),this.prototype.hasOwnProperty(e)&&((t=Object.create(t)).wrapped=!0),this.elementProperties.set(e,t),!t.noAccessor){let n=Symbol(),r=this.getPropertyDescriptor(e,n,t);r!==void 0&&d(this.prototype,e,r)}}static getPropertyDescriptor(e,t,n){let{get:r,set:i}=ee(this.prototype,e)??{get(){return this[t]},set(e){this[t]=e}};return{get:r,set(t){let a=r?.call(this);i?.call(this,t),this.requestUpdate(e,a,n)},configurable:!0,enumerable:!0}}static getPropertyOptions(e){return this.elementProperties.get(e)??g}static _$Ei(){if(this.hasOwnProperty(p(`elementProperties`)))return;let e=re(this);e.finalize(),e.l!==void 0&&(this.l=[...e.l]),this.elementProperties=new Map(e.elementProperties)}static finalize(){if(this.hasOwnProperty(p(`finalized`)))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(p(`properties`))){let e=this.properties,t=[...te(e),...ne(e)];for(let n of t)this.createProperty(n,e[n])}let e=this[Symbol.metadata];if(e!==null){let t=litPropertyMetadata.get(e);if(t!==void 0)for(let[e,n]of t)this.elementProperties.set(e,n)}this._$Eh=new Map;for(let[e,t]of this.elementProperties){let n=this._$Eu(e,t);n!==void 0&&this._$Eh.set(n,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(e){let t=[];if(Array.isArray(e)){let n=new Set(e.flat(1/0).reverse());for(let e of n)t.unshift(l(e))}else e!==void 0&&t.push(l(e));return t}static _$Eu(e,t){let n=t.attribute;return!1===n?void 0:typeof n==`string`?n:typeof e==`string`?e.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(e=>this.enableUpdating=e),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(e=>e(this))}addController(e){(this._$EO??=new Set).add(e),this.renderRoot!==void 0&&this.isConnected&&e.hostConnected?.()}removeController(e){this._$EO?.delete(e)}_$E_(){let e=new Map,t=this.constructor.elementProperties;for(let n of t.keys())this.hasOwnProperty(n)&&(e.set(n,this[n]),delete this[n]);e.size>0&&(this._$Ep=e)}createRenderRoot(){let e=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return c(e,this.constructor.elementStyles),e}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(e=>e.hostConnected?.())}enableUpdating(e){}disconnectedCallback(){this._$EO?.forEach(e=>e.hostDisconnected?.())}attributeChangedCallback(e,t,n){this._$AK(e,n)}_$ET(e,t){let n=this.constructor.elementProperties.get(e),r=this.constructor._$Eu(e,n);if(r!==void 0&&!0===n.reflect){let i=(n.converter?.toAttribute===void 0?m:n.converter).toAttribute(t,n.type);this._$Em=e,i==null?this.removeAttribute(r):this.setAttribute(r,i),this._$Em=null}}_$AK(e,t){let n=this.constructor,r=n._$Eh.get(e);if(r!==void 0&&this._$Em!==r){let e=n.getPropertyOptions(r),i=typeof e.converter==`function`?{fromAttribute:e.converter}:e.converter?.fromAttribute===void 0?m:e.converter;this._$Em=r;let a=i.fromAttribute(t,e.type);this[r]=a??this._$Ej?.get(r)??a,this._$Em=null}}requestUpdate(e,t,n,r=!1,i){if(e!==void 0){let a=this.constructor;if(!1===r&&(i=this[e]),n??=a.getPropertyOptions(e),!((n.hasChanged??h)(i,t)||n.useDefault&&n.reflect&&i===this._$Ej?.get(e)&&!this.hasAttribute(a._$Eu(e,n))))return;this.C(e,t,n)}!1===this.isUpdatePending&&(this._$ES=this._$EP())}C(e,t,{useDefault:n,reflect:r,wrapped:i},a){n&&!(this._$Ej??=new Map).has(e)&&(this._$Ej.set(e,a??t??this[e]),!0!==i||a!==void 0)||(this._$AL.has(e)||(this.hasUpdated||n||(t=void 0),this._$AL.set(e,t)),!0===r&&this._$Em!==e&&(this._$Eq??=new Set).add(e))}async _$EP(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let e=this.scheduleUpdate();return e!=null&&await e,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[e,t]of this._$Ep)this[e]=t;this._$Ep=void 0}let e=this.constructor.elementProperties;if(e.size>0)for(let[t,n]of e){let{wrapped:e}=n,r=this[t];!0!==e||this._$AL.has(t)||r===void 0||this.C(t,void 0,n,r)}}let e=!1,t=this._$AL;try{e=this.shouldUpdate(t),e?(this.willUpdate(t),this._$EO?.forEach(e=>e.hostUpdate?.()),this.update(t)):this._$EM()}catch(t){throw e=!1,this._$EM(),t}e&&this._$AE(t)}willUpdate(e){}_$AE(e){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(e)),this.updated(e)}_$EM(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(e){return!0}update(e){this._$Eq&&=this._$Eq.forEach(e=>this._$ET(e,this[e])),this._$EM()}updated(e){}firstUpdated(e){}};_.elementStyles=[],_.shadowRootOptions={mode:`open`},_[p(`elementProperties`)]=new Map,_[p(`finalized`)]=new Map,oe?.({ReactiveElement:_}),(f.reactiveElementVersions??=[]).push(`2.1.2`);var v=globalThis,y=e=>e,b=v.trustedTypes,x=b?b.createPolicy(`lit-html`,{createHTML:e=>e}):void 0,S=`$lit$`,C=`lit$${Math.random().toFixed(9).slice(2)}$`,w=`?`+C,se=`<${w}>`,T=document,E=()=>T.createComment(``),D=e=>e===null||typeof e!=`object`&&typeof e!=`function`,O=Array.isArray,ce=e=>O(e)||typeof e?.[Symbol.iterator]==`function`,k=`[ 	
\f\r]`,A=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,le=/-->/g,ue=/>/g,j=RegExp(`>|${k}(?:([^\\s"'>=/]+)(${k}*=${k}*(?:[^ \t\n\f\r"'\`<>=]|("|')|))|$)`,`g`),M=/'/g,N=/"/g,P=/^(?:script|style|textarea|title)$/i,F=(e=>(t,...n)=>({_$litType$:e,strings:t,values:n}))(1),I=Symbol.for(`lit-noChange`),L=Symbol.for(`lit-nothing`),R=new WeakMap,z=T.createTreeWalker(T,129);function B(e,t){if(!O(e)||!e.hasOwnProperty(`raw`))throw Error(`invalid template strings array`);return x===void 0?t:x.createHTML(t)}var de=(e,t)=>{let n=e.length-1,r=[],i,a=t===2?`<svg>`:t===3?`<math>`:``,o=A;for(let t=0;t<n;t++){let n=e[t],s,c,l=-1,u=0;for(;u<n.length&&(o.lastIndex=u,c=o.exec(n),c!==null);)u=o.lastIndex,o===A?c[1]===`!--`?o=le:c[1]===void 0?c[2]===void 0?c[3]!==void 0&&(o=j):(P.test(c[2])&&(i=RegExp(`</`+c[2],`g`)),o=j):o=ue:o===j?c[0]===`>`?(o=i??A,l=-1):c[1]===void 0?l=-2:(l=o.lastIndex-c[2].length,s=c[1],o=c[3]===void 0?j:c[3]===`"`?N:M):o===N||o===M?o=j:o===le||o===ue?o=A:(o=j,i=void 0);let d=o===j&&e[t+1].startsWith(`/>`)?` `:``;a+=o===A?n+se:l>=0?(r.push(s),n.slice(0,l)+S+n.slice(l)+C+d):n+C+(l===-2?t:d)}return[B(e,a+(e[n]||`<?>`)+(t===2?`</svg>`:t===3?`</math>`:``)),r]},V=class e{constructor({strings:t,_$litType$:n},r){let i;this.parts=[];let a=0,o=0,s=t.length-1,c=this.parts,[l,u]=de(t,n);if(this.el=e.createElement(l,r),z.currentNode=this.el.content,n===2||n===3){let e=this.el.content.firstChild;e.replaceWith(...e.childNodes)}for(;(i=z.nextNode())!==null&&c.length<s;){if(i.nodeType===1){if(i.hasAttributes())for(let e of i.getAttributeNames())if(e.endsWith(S)){let t=u[o++],n=i.getAttribute(e).split(C),r=/([.?@])?(.*)/.exec(t);c.push({type:1,index:a,name:r[2],strings:n,ctor:r[1]===`.`?pe:r[1]===`?`?me:r[1]===`@`?he:W}),i.removeAttribute(e)}else e.startsWith(C)&&(c.push({type:6,index:a}),i.removeAttribute(e));if(P.test(i.tagName)){let e=i.textContent.split(C),t=e.length-1;if(t>0){i.textContent=b?b.emptyScript:``;for(let n=0;n<t;n++)i.append(e[n],E()),z.nextNode(),c.push({type:2,index:++a});i.append(e[t],E())}}}else if(i.nodeType===8){if(i.data===w)c.push({type:2,index:a});else{let e=-1;for(;(e=i.data.indexOf(C,e+1))!==-1;)c.push({type:7,index:a}),e+=C.length-1}}a++}}static createElement(e,t){let n=T.createElement(`template`);return n.innerHTML=e,n}};function H(e,t,n=e,r){if(t===I)return t;let i=r===void 0?n._$Cl:n._$Co?.[r],a=D(t)?void 0:t._$litDirective$;return i?.constructor!==a&&(i?._$AO?.(!1),a===void 0?i=void 0:(i=new a(e),i._$AT(e,n,r)),r===void 0?n._$Cl=i:(n._$Co??=[])[r]=i),i!==void 0&&(t=H(e,i._$AS(e,t.values),i,r)),t}var fe=class{constructor(e,t){this._$AV=[],this._$AN=void 0,this._$AD=e,this._$AM=t}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(e){let{el:{content:t},parts:n}=this._$AD,r=(e?.creationScope??T).importNode(t,!0);z.currentNode=r;let i=z.nextNode(),a=0,o=0,s=n[0];for(;s!==void 0;){if(a===s.index){let t;s.type===2?t=new U(i,i.nextSibling,this,e):s.type===1?t=new s.ctor(i,s.name,s.strings,this,e):s.type===6&&(t=new ge(i,this,e)),this._$AV.push(t),s=n[++o]}a!==s?.index&&(i=z.nextNode(),a++)}return z.currentNode=T,r}p(e){let t=0;for(let n of this._$AV)n!==void 0&&(n.strings===void 0?n._$AI(e[t]):(n._$AI(e,n,t),t+=n.strings.length-2)),t++}},U=class e{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(e,t,n,r){this.type=2,this._$AH=L,this._$AN=void 0,this._$AA=e,this._$AB=t,this._$AM=n,this.options=r,this._$Cv=r?.isConnected??!0}get parentNode(){let e=this._$AA.parentNode,t=this._$AM;return t!==void 0&&e?.nodeType===11&&(e=t.parentNode),e}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(e,t=this){e=H(this,e,t),D(e)?e===L||e==null||e===``?(this._$AH!==L&&this._$AR(),this._$AH=L):e!==this._$AH&&e!==I&&this._(e):e._$litType$===void 0?e.nodeType===void 0?ce(e)?this.k(e):this._(e):this.T(e):this.$(e)}O(e){return this._$AA.parentNode.insertBefore(e,this._$AB)}T(e){this._$AH!==e&&(this._$AR(),this._$AH=this.O(e))}_(e){this._$AH!==L&&D(this._$AH)?this._$AA.nextSibling.data=e:this.T(T.createTextNode(e)),this._$AH=e}$(e){let{values:t,_$litType$:n}=e,r=typeof n==`number`?this._$AC(e):(n.el===void 0&&(n.el=V.createElement(B(n.h,n.h[0]),this.options)),n);if(this._$AH?._$AD===r)this._$AH.p(t);else{let e=new fe(r,this),n=e.u(this.options);e.p(t),this.T(n),this._$AH=e}}_$AC(e){let t=R.get(e.strings);return t===void 0&&R.set(e.strings,t=new V(e)),t}k(t){O(this._$AH)||(this._$AH=[],this._$AR());let n=this._$AH,r,i=0;for(let a of t)i===n.length?n.push(r=new e(this.O(E()),this.O(E()),this,this.options)):r=n[i],r._$AI(a),i++;i<n.length&&(this._$AR(r&&r._$AB.nextSibling,i),n.length=i)}_$AR(e=this._$AA.nextSibling,t){for(this._$AP?.(!1,!0,t);e!==this._$AB;){let t=y(e).nextSibling;y(e).remove(),e=t}}setConnected(e){this._$AM===void 0&&(this._$Cv=e,this._$AP?.(e))}},W=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(e,t,n,r,i){this.type=1,this._$AH=L,this._$AN=void 0,this.element=e,this.name=t,this._$AM=r,this.options=i,n.length>2||n[0]!==``||n[1]!==``?(this._$AH=Array(n.length-1).fill(new String),this.strings=n):this._$AH=L}_$AI(e,t=this,n,r){let i=this.strings,a=!1;if(i===void 0)e=H(this,e,t,0),a=!D(e)||e!==this._$AH&&e!==I,a&&(this._$AH=e);else{let r=e,o,s;for(e=i[0],o=0;o<i.length-1;o++)s=H(this,r[n+o],t,o),s===I&&(s=this._$AH[o]),a||=!D(s)||s!==this._$AH[o],s===L?e=L:e!==L&&(e+=(s??``)+i[o+1]),this._$AH[o]=s}a&&!r&&this.j(e)}j(e){e===L?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,e??``)}},pe=class extends W{constructor(){super(...arguments),this.type=3}j(e){this.element[this.name]=e===L?void 0:e}},me=class extends W{constructor(){super(...arguments),this.type=4}j(e){this.element.toggleAttribute(this.name,!!e&&e!==L)}},he=class extends W{constructor(e,t,n,r,i){super(e,t,n,r,i),this.type=5}_$AI(e,t=this){if((e=H(this,e,t,0)??L)===I)return;let n=this._$AH,r=e===L&&n!==L||e.capture!==n.capture||e.once!==n.once||e.passive!==n.passive,i=e!==L&&(n===L||r);r&&this.element.removeEventListener(this.name,this,n),i&&this.element.addEventListener(this.name,this,e),this._$AH=e}handleEvent(e){typeof this._$AH==`function`?this._$AH.call(this.options?.host??this.element,e):this._$AH.handleEvent(e)}},ge=class{constructor(e,t,n){this.element=e,this.type=6,this._$AN=void 0,this._$AM=t,this.options=n}get _$AU(){return this._$AM._$AU}_$AI(e){H(this,e)}},_e=v.litHtmlPolyfillSupport;_e?.(V,U),(v.litHtmlVersions??=[]).push(`3.3.3`);var ve=(e,t,n)=>{let r=n?.renderBefore??t,i=r._$litPart$;if(i===void 0){let e=n?.renderBefore??null;r._$litPart$=i=new U(t.insertBefore(E(),e),e,void 0,n??{})}return i._$AI(e),i},G=globalThis,K=class extends _{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){let e=super.createRenderRoot();return this.renderOptions.renderBefore??=e.firstChild,e}update(e){let t=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(e),this._$Do=ve(t,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return I}};K._$litElement$=!0,K.finalized=!0,G.litElementHydrateSupport?.({LitElement:K});var ye=G.litElementPolyfillSupport;ye?.({LitElement:K}),(G.litElementVersions??=[]).push(`4.2.2`);var be={attribute:!0,type:String,converter:m,reflect:!1,hasChanged:h},xe=(e=be,t,n)=>{let{kind:r,metadata:i}=n,a=globalThis.litPropertyMetadata.get(i);if(a===void 0&&globalThis.litPropertyMetadata.set(i,a=new Map),r===`setter`&&((e=Object.create(e)).wrapped=!0),a.set(n.name,e),r===`accessor`){let{name:r}=n;return{set(n){let i=t.get.call(this);t.set.call(this,n),this.requestUpdate(r,i,e,!0,n)},init(t){return t!==void 0&&this.C(r,void 0,e,t),t}}}if(r===`setter`){let{name:r}=n;return function(n){let i=this[r];t.call(this,n),this.requestUpdate(r,i,e,!0,n)}}throw Error(`Unsupported decorator location: `+r)};function q(e){return(t,n)=>typeof n==`object`?xe(e,t,n):((e,t,n)=>{let r=t.hasOwnProperty(n);return t.constructor.createProperty(n,e),r?Object.getOwnPropertyDescriptor(t,n):void 0})(e,t,n)}function J(e){return q({...e,state:!0,attribute:!1})}function Y(e,t,n,r){var i=arguments.length,a=i<3?t:r===null?r=Object.getOwnPropertyDescriptor(t,n):r,o;if(typeof Reflect==`object`&&typeof Reflect.decorate==`function`)a=Reflect.decorate(e,t,n,r);else for(var s=e.length-1;s>=0;s--)(o=e[s])&&(a=(i<3?o(a):i>3?o(t,n,a):o(t,n))||a);return i>3&&a&&Object.defineProperty(t,n,a),a}var X=`/apis/api.link.halo.run/v1alpha1/link-applications`,Se=`${X}/captcha`;function Ce(e,t){let n=String(e.get(`rssUrl`)||``).trim();return{url:String(e.get(`url`)||``).trim(),displayName:String(e.get(`displayName`)||``).trim(),logo:String(e.get(`logo`)||``).trim()||null,description:String(e.get(`description`)||``).trim()||null,email:String(e.get(`email`)||``).trim()||null,backlink:String(e.get(`backlink`)||``).trim()||null,feedUrls:n?[n]:[],challengeId:t,captchaCode:String(e.get(`captchaCode`)||``).trim()}}async function Z(e){let t=await e.text();if(!t)return null;try{return JSON.parse(t)}catch{throw Error(`接口返回格式错误`)}}var Q=class extends K{constructor(){super(),this.open=!1,this.submitting=!1,this.fetchingSite=!1,this.sitePreviewEnabled=!0,this.captchaLoading=!1,this.toastMessage=``,this.toastType=`success`,this.siteInfoRequest=0,this.captchaRequest=0,this.previousBodyOverflow=``,this.focusOrigin=null,this.lastFetchedSiteInfo={title:``,logo:``,description:``},this.fetchConfiguration()}willUpdate(e){if(e.has(`open`)){if(this.open){let e=document.activeElement;this.focusOrigin=e instanceof HTMLElement?e:null,this.previousBodyOverflow=document.body.style.overflow,document.body.style.overflow=`hidden`,this.captcha||this.fetchCaptcha()}else{document.body.style.overflow=this.previousBodyOverflow;let e=this.focusOrigin;this.focusOrigin=null,e?.isConnected&&requestAnimationFrame(()=>e.focus())}}}updated(e){e.has(`open`)&&this.open&&requestAnimationFrame(()=>this.shadowRoot?.querySelector(`#input-url`)?.focus())}disconnectedCallback(){this.siteInfoRequest+=1,this.captchaRequest+=1,window.clearTimeout(this.toastTimer),window.clearTimeout(this.captchaExpiryTimer),document.body.style.overflow=this.previousBodyOverflow,super.disconnectedCallback()}showToast(e,t=`success`){window.clearTimeout(this.toastTimer),this.toastMessage=e,this.toastType=t,this.toastTimer=window.setTimeout(()=>{this.toastMessage=``},3600)}async fetchConfiguration(){try{let e=await fetch(`/apis/api.link.submit.halo.run/v1alpha1/configuration`,{credentials:`same-origin`,headers:{Accept:`application/json`}});if(!e.ok)return;let t=await Z(e);this.sitePreviewEnabled=t?.linkPreviewEnabled!==!1}catch(e){console.error(`Failed to load link enhancement configuration:`,e)}}async fetchCaptcha(){let e=++this.captchaRequest;this.captchaLoading=!0,window.clearTimeout(this.captchaExpiryTimer);try{let t=await fetch(Se,{method:`POST`,credentials:`same-origin`,headers:{Accept:`application/json`},cache:`no-store`}),n=await Z(t);if(e!==this.captchaRequest)return;if(!t.ok||!n?.challengeId||!n.image)throw Error(n?.detail||n?.title||`验证码加载失败`);this.captcha=n;let r=Math.max(15,n.expiresInSeconds-10)*1e3;this.captchaExpiryTimer=window.setTimeout(()=>{this.captcha=void 0,this.open&&this.fetchCaptcha()},r)}catch(t){e===this.captchaRequest&&(this.captcha=void 0,this.showToast(t instanceof Error?t.message:`验证码加载失败`,`error`))}finally{e===this.captchaRequest&&(this.captchaLoading=!1)}}async fetchSiteInfo(){let e=this.shadowRoot?.querySelector(`#input-url`);if(!e?.value.trim()){this.showToast(`请先填写网址`,`error`);return}let t=e.value.trim();/^https?:\/\//i.test(t)||(t=`https://${t}`,e.value=t);let n=++this.siteInfoRequest;this.fetchingSite=!0;try{let e=await fetch(`/apis/api.link.submit.halo.run/v1alpha1/site-info?url=${encodeURIComponent(t)}`,{credentials:`same-origin`,headers:{Accept:`application/json`},cache:`no-store`}),r=await Z(e);if(n!==this.siteInfoRequest)return;if(!e.ok){this.showToast(r?.detail||`获取网站信息失败，请手动填写`,`error`);return}this.fillSiteInfo(r?.title,r?.logo,r?.description),this.showToast(r?.title||r?.description||r?.logo?`已自动填充网站信息`:`未获取到网站信息，请手动填写`,r?.title||r?.description||r?.logo?`success`:`error`)}catch(e){n===this.siteInfoRequest&&this.showToast(e instanceof Error?e.message:`获取网站信息失败`,`error`)}finally{n===this.siteInfoRequest&&(this.fetchingSite=!1)}}fillSiteInfo(e,t,n){let r=this.shadowRoot?.querySelector(`#input-name`),i=this.shadowRoot?.querySelector(`#input-logo`),a=this.shadowRoot?.querySelector(`#textarea-description`);r&&this.replaceFetchedValue(r,e,this.lastFetchedSiteInfo.title),i&&this.replaceFetchedValue(i,t,this.lastFetchedSiteInfo.logo),a&&this.replaceFetchedValue(a,n,this.lastFetchedSiteInfo.description),this.lastFetchedSiteInfo={title:e?.trim()||``,logo:t?.trim()||``,description:n?.trim()||``}}replaceFetchedValue(e,t,n){let r=e.value.trim();(!r||r===n)&&(e.value=t?.trim()||``)}handleClose(){this.siteInfoRequest+=1,this.fetchingSite=!1,this.open=!1}handleKeydown(e){if(e.key===`Escape`){e.preventDefault(),this.handleClose();return}if(e.key!==`Tab`)return;let t=this.shadowRoot?.querySelector(`.modal-content`),n=Array.from(t?.querySelectorAll(`button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [href]`)??[]).filter(e=>!e.hidden&&e.getAttribute(`aria-hidden`)!==`true`);if(!n.length)return;let r=this.shadowRoot?.activeElement,i=n[0],a=n[n.length-1];e.shiftKey&&r===i?(e.preventDefault(),a.focus()):!e.shiftKey&&r===a&&(e.preventDefault(),i.focus())}async handleSubmit(e){if(e.preventDefault(),!this.captcha?.challengeId){this.showToast(`验证码尚未加载，请刷新后重试`,`error`);return}this.submitting=!0;let t=e.currentTarget;try{let e=await fetch(X,{method:`POST`,credentials:`same-origin`,headers:{"Content-Type":`application/json`,Accept:`application/json, application/problem+json`},body:JSON.stringify(Ce(new FormData(t),this.captcha.challengeId))}),n=await Z(e);if(!e.ok){this.showToast(n?.detail||n?.title||`提交失败，请检查表单`,`error`),await this.fetchCaptcha();return}this.showToast(`申请已提交到官方友链审核，请等待处理`),t.reset(),this.captcha=void 0,window.setTimeout(()=>this.handleClose(),1200)}catch(e){this.showToast(e instanceof Error?e.message:`提交失败，请稍后重试`,`error`),await this.fetchCaptcha()}finally{this.submitting=!1}}renderForm(){return F`
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
            ${this.sitePreviewEnabled?F`<button
                    type="button"
                    class="secondary-button"
                    ?disabled=${this.fetchingSite}
                    @click=${this.fetchSiteInfo}
                  >
                    ${this.fetchingSite?`获取中...`:`获取信息`}
                  </button>`:``}
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
            ${this.captcha?.image?F`<img src=${this.captcha.image} alt="友链申请验证码" />`:F`<span>${this.captchaLoading?`加载中`:`点击刷新`}</span>`}
          </button>
        </div>

        <footer class="modal-footer">
          <button type="button" class="secondary-button" @click=${this.handleClose}>取消</button>
          <button
            type="submit"
            class="primary-button"
            ?disabled=${this.submitting||this.captchaLoading||!this.captcha}
          >
            ${this.submitting?`提交中...`:`提交申请`}
          </button>
        </footer>
      </form>
    `}render(){let e=this.open?`false`:`true`;return F`
      <div
        class="modal-wrapper ${this.open?`is-open`:``}"
        aria-hidden=${e}
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
          ${this.open?this.renderForm():``}
        </section>
        ${this.toastMessage?F`<div
                class="toast ${this.toastType}"
                role=${this.toastType===`error`?`alert`:`status`}
                aria-live="polite"
              >
                <strong>${this.toastType===`error`?`提交提示`:`操作成功`}</strong>
                <span>${this.toastMessage}</span>
              </div>`:``}
      </div>
    `}static{this.styles=s`
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
  `}};Y([q({type:Boolean,reflect:!0})],Q.prototype,`open`,void 0),Y([J()],Q.prototype,`submitting`,void 0),Y([J()],Q.prototype,`fetchingSite`,void 0),Y([J()],Q.prototype,`sitePreviewEnabled`,void 0),Y([J()],Q.prototype,`captchaLoading`,void 0),Y([J()],Q.prototype,`captcha`,void 0),Y([J()],Q.prototype,`toastMessage`,void 0),Y([J()],Q.prototype,`toastType`,void 0),customElements.get(`link-submit-modal`)||customElements.define(`link-submit-modal`,Q);var $=document.createElement(`link-submit-modal`);document.body.append($);function we(){let e=document.querySelector(`[data-link-application-enabled]`);return!e||e.dataset.linkApplicationEnabled!==`false`}function Te(){we()&&($.open=!0)}we()||document.querySelectorAll(`[data-link-submit-widget-trigger]`).forEach(e=>{e.hidden=!0,e.setAttribute(`aria-hidden`,`true`)}),document.querySelectorAll(`[data-link-submit-widget-trigger]`).forEach(e=>{e.addEventListener(`click`,Te)}),e.LinkSubmitModal=Q,e.open=Te})(this.LinkSubmitWidget=this.LinkSubmitWidget||{});
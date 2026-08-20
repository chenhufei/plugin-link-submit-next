import { LinkSubmitModal } from './link-submit-modal';

export { LinkSubmitModal };

const linkSubmitModalElement = document.createElement('link-submit-modal') as LinkSubmitModal;

document.body.append(linkSubmitModalElement);

function isOfficialApplicationEnabled() {
  const marker = document.querySelector<HTMLElement>('[data-link-application-enabled]');
  return !marker || marker.dataset.linkApplicationEnabled !== 'false';
}

export function open() {
  if (!isOfficialApplicationEnabled()) return;
  linkSubmitModalElement.open = true;
}

if (!isOfficialApplicationEnabled()) {
  document.querySelectorAll<HTMLElement>('[data-link-submit-widget-trigger]').forEach((trigger) => {
    trigger.hidden = true;
    trigger.setAttribute('aria-hidden', 'true');
  });
}

document.querySelectorAll<HTMLElement>('[data-link-submit-widget-trigger]').forEach((trigger) => {
  trigger.addEventListener('click', open);
});

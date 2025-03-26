import React from "react";
import { useState, useCallback } from "react";
import ReactDOMServer from "react-dom/server";

export const useCenteredTree = (defaultTranslate = { x: 0, y: 0 }) => {
  const [translate, setTranslate] = useState<{ x: number; y: number }>(defaultTranslate);
  const [dimensions, setDimensions] = useState<{ width: number; height: number }>({ width: 0, height: 0 });

  const containerRef = useCallback((containerElem: HTMLDivElement | null) => {
    if (containerElem !== null) {
      const { width, height } = containerElem.getBoundingClientRect();
      setDimensions({ width, height });
      setTranslate({ x: width / 3, y: height / 2 });
    }
  }, []);

  return [dimensions, translate, containerRef] as const; // Merk: `as const` sikrer faste typer
};


export const extractSvgContent = (icon: JSX.Element | null): JSX.Element[] => {
  if (icon === null) {
    return [];
  }
  const svgMarkup = ReactDOMServer.renderToString(icon);
  const parsedSvg = new DOMParser().parseFromString(svgMarkup, 'image/svg+xml');
  
  // Hent viewBox-attributtet fra SVG
  const svgElement = parsedSvg.documentElement;
  const viewBox = svgElement.getAttribute('viewBox');
  
  if (!viewBox) {
    throw new Error('SVG mangler viewBox-attributt');
  }

  // Ekstraher minX, minY, width, height fra viewBox
  const [minX, minY, width, height] = viewBox.split(' ').map(Number);
  
  // Beregn offset for senteret og sett det som negativt transform
  const offsetX = -(minX + width / 2);
  const offsetY = -(minY + height / 2);

  return Array.from(parsedSvg.documentElement.children).map((child, index) => {
    const props = {
      key: index,
      transform: `translate(${offsetX}, ${offsetY})`, // Juster transform med negativt offset
      ...Array.from(child.attributes).reduce((acc, { name, value }) => ({ ...acc, [name]: value }), {}),
    };

    return React.createElement(child.tagName, props, null);
  });
};
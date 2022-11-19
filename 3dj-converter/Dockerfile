FROM node:16.17

WORKDIR /build

COPY ["package.json", "yarn.lock", "./"]
RUN yarn install

COPY . .
ENV NODE_ENV=production
RUN yarn run build

RUN mkdir out
CMD cp -r dist/* out/
